package app;

import auth.AuthorizationFilter;
import auth.JwtTokenProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import db.DBConnection;
import film.FilmService;
import jury.EvaluationService;
import jury.JuryAssignmentService;
import user.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiServer {
    public static void register(HttpServer server) {
        server.createContext("/api", new ApiHandler());
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equals(method)) {
                handleOptions(exchange);
                return;
            }

            System.out.println("[DEBUG] API Request: " + method + " " + path);

            String[] parts = path.split("/");
            if (parts.length < 3) {
                sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Not found"));
                return;
            }

            String resource = parts[2];
            String subResource = parts.length >= 4 ? parts[3] : null;

            try {
                if ("auth".equals(resource)) {
                    handleAuth(exchange, subResource);
                    return;
                }

                AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

                switch (resource) {
                    case "films" -> handleFilms(exchange, subResource, auth);
                    case "screenings" -> handleScreenings(exchange, subResource, auth);
                    case "admin" -> handleAdmin(exchange, subResource, auth);
                    case "jury" -> handleJury(exchange, subResource, auth);
                    case "user" -> handleUser(exchange, subResource, auth);
                    case "attendees" -> handleAttendees(exchange, subResource);
                    case "awards" -> handleAwards(exchange, subResource);
                    case "venues" -> handleVenues(exchange, subResource);
                    case "filmcrew" -> handleFilmCrew(exchange, subResource);
                    case "tickets" -> handleTickets(exchange, subResource);
                    default -> sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND,
                        errorJson("Resource not found"));
                }
            } catch (SQLException ex) {
                System.out.println("[ERROR] SQLException: " + ex.getMessage());
                ex.printStackTrace();
                sendJson(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, errorJson(ex.getMessage()));
            } catch (Exception ex) {
                System.out.println("[ERROR] Unexpected exception: " + ex.getMessage());
                ex.printStackTrace();
                sendJson(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
                    errorJson("Server error: " + ex.getMessage()));
            }
        }
    }

    private static void handleAuth(HttpExchange exchange, String action) throws IOException, SQLException {
        String method = exchange.getRequestMethod();

        if ("POST".equals(method) && "register".equals(action)) {
            register(exchange);
        } else if ("POST".equals(method) && "login".equals(action)) {
            login(exchange);
        } else if ("GET".equals(method) && "me".equals(action)) {
            getCurrentUser(exchange);
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
        }
    }

    private static void register(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));

        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String role = body.getOrDefault("role", "USER");

        if (isBlank(name) || isBlank(email) || isBlank(password)) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                errorJson("Missing required fields: name, email, password"));
            return;
        }

        try {
            UserService.UserDTO user = UserService.registerUser(name, email, password, role);
            String token = JwtTokenProvider.generateToken(user.userId, user.email, user.role);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("token", token);
            response.put("user", mapOf(
                "user_id", user.userId,
                "name", user.name,
                "email", user.email,
                "role", user.role
            ));

            sendJson(exchange, HttpURLConnection.HTTP_CREATED, toJsonObject(response));
        } catch (SQLException e) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson(e.getMessage()));
        }
    }

    private static void login(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));

        String email = body.get("email");
        String password = body.get("password");

        if (isBlank(email) || isBlank(password)) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson("Missing email or password"));
            return;
        }

        try {
            UserService.UserDTO user = UserService.loginUser(email, password);
            String token = JwtTokenProvider.generateToken(user.userId, user.email, user.role);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", mapOf(
                "user_id", user.userId,
                "name", user.name,
                "email", user.email,
                "role", user.role
            ));

            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(response));
        } catch (SQLException e) {
            sendJson(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, errorJson("Invalid credentials"));
        }
    }

    private static void getCurrentUser(HttpExchange exchange) throws IOException {
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);
        if (!auth.isAuthenticated) {
            sendJson(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, errorJson(auth.error));
            return;
        }

        try {
            UserService.UserDTO user = UserService.getUserById(auth.userId);
            if (user == null) {
                sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("User not found"));
                return;
            }

            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                "user_id", user.userId,
                "name", user.name,
                "email", user.email,
                "role", user.role
            )));
        } catch (SQLException e) {
            sendJson(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, errorJson(e.getMessage()));
        }
    }

    private static void handleAdmin(HttpExchange exchange, String action,
                                    AuthorizationFilter.AuthResult auth) throws IOException, SQLException {
        if (!ensureAuthenticated(exchange, auth)) {
            return;
        }
        if (!AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        String method = exchange.getRequestMethod();

        if ("POST".equals(method) && "assign-jury".equals(action)) {
            assignJury(exchange);
        } else if ("GET".equals(method) && "jury-assignments".equals(action)) {
            getJuryAssignments(exchange);
        } else if ("GET".equals(method) && "evaluations".equals(action)) {
            getAllEvaluations(exchange);
        } else if ("GET".equals(method) && "award-eligible".equals(action)) {
            getAwardEligibleFilms(exchange);
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
        }
    }

    private static void assignJury(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));

        Integer juryId = parseInt(body.get("jury_id"), null);
        List<Integer> filmIds = parseIntegerList(body.get("film_ids"));

        if (juryId == null || filmIds.isEmpty()) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST,
                errorJson("Missing jury_id or film_ids"));
            return;
        }

        try {
            List<JuryAssignmentService.JuryAssignmentDTO> assignments =
                JuryAssignmentService.assignJuryToMultipleFilms(juryId, filmIds);

            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                "success", true,
                "assigned_count", assignments.size()
            )));
        } catch (SQLException e) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson(e.getMessage()));
        }
    }

    private static void getJuryAssignments(HttpExchange exchange) throws IOException, SQLException {
        List<JuryAssignmentService.JuryAssignmentDTO> assignments = JuryAssignmentService.getAllAssignments();

        List<Map<String, Object>> result = new ArrayList<>();
        for (JuryAssignmentService.JuryAssignmentDTO assignment : assignments) {
            result.add(mapOf(
                "id", assignment.assignmentId,
                "jury_id", assignment.juryId,
                "film_id", assignment.filmId,
                "film_title", assignment.filmTitle,
                "assigned_at", toIsoString(assignment.assignedAt)
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void getAllEvaluations(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT e.evaluation_id, e.jury_id, e.film_id, f.title, e.score, " +
                "e.remarks, e.created_at FROM evaluation e " +
                "JOIN film f ON e.film_id = f.film_id ORDER BY e.created_at DESC";
            PreparedStatement ps = con.prepareStatement(sql);

            List<Map<String, Object>> evaluations = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapOf(
                        "evaluation_id", rs.getInt("evaluation_id"),
                        "jury_id", rs.getInt("jury_id"),
                        "film_id", rs.getInt("film_id"),
                        "film_title", rs.getString("title"),
                        "score", rs.getInt("score"),
                        "remarks", rs.getString("remarks"),
                        "created_at", toIsoString(rs.getTimestamp("created_at"))
                    ));
                }
            }

            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(evaluations));
        }
    }

    private static void getAwardEligibleFilms(HttpExchange exchange) throws IOException, SQLException {
        List<FilmService.FilmDTO> films = FilmService.getAwardEligibleFilms();

        List<Map<String, Object>> result = new ArrayList<>();
        for (FilmService.FilmDTO film : films) {
            result.add(mapOf(
                "film_id", film.filmId,
                "title", film.title,
                "genre", film.genre,
                "language", film.language,
                "avg_score", film.averageScore,
                "evaluation_count", film.evaluationCount
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void handleJury(HttpExchange exchange, String action,
                                   AuthorizationFilter.AuthResult auth) throws IOException, SQLException {
        if (!ensureAuthenticated(exchange, auth)) {
            return;
        }
        if (!AuthorizationFilter.hasRole(auth, "JURY")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Jury access required"));
            return;
        }

        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && "assigned-films".equals(action)) {
            getAssignedFilms(exchange, auth.userId);
        } else if ("POST".equals(method) && "evaluate".equals(action)) {
            submitEvaluation(exchange, auth.userId);
        } else if ("GET".equals(method) && "evaluations".equals(action)) {
            getMyEvaluations(exchange, auth.userId);
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
        }
    }

    private static void getAssignedFilms(HttpExchange exchange, Integer userId) throws IOException, SQLException {
        Integer juryId = getJuryIdForUser(userId);
        if (juryId == null) {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Jury record not found"));
            return;
        }

        List<JuryAssignmentService.JuryAssignmentDTO> assignments =
            JuryAssignmentService.getJuryAssignments(juryId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (JuryAssignmentService.JuryAssignmentDTO assignment : assignments) {
            result.add(mapOf(
                "film_id", assignment.filmId,
                "title", assignment.filmTitle,
                "assigned_at", toIsoString(assignment.assignedAt)
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void submitEvaluation(HttpExchange exchange, Integer userId) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));

        Integer filmId = parseInt(body.get("film_id"), null);
        Integer score = parseInt(body.get("score"), null);
        String remarks = body.get("remarks");

        if (filmId == null || score == null) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson("Missing film_id or score"));
            return;
        }

        Integer juryId = getJuryIdForUser(userId);
        if (juryId == null) {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Jury record not found"));
            return;
        }

        try {
            EvaluationService.EvaluationDTO evaluation =
                EvaluationService.submitEvaluation(juryId, filmId, score, remarks);

            sendJson(exchange, HttpURLConnection.HTTP_CREATED, toJsonObject(mapOf(
                "success", true,
                "evaluation_id", evaluation.evaluationId,
                "message", "Evaluation submitted successfully"
            )));
        } catch (SQLException e) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson(e.getMessage()));
        }
    }

    private static void getMyEvaluations(HttpExchange exchange, Integer userId) throws IOException, SQLException {
        Integer juryId = getJuryIdForUser(userId);
        if (juryId == null) {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Jury record not found"));
            return;
        }

        List<EvaluationService.EvaluationDTO> evaluations = EvaluationService.getJuryEvaluations(juryId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (EvaluationService.EvaluationDTO evaluation : evaluations) {
            result.add(mapOf(
                "evaluation_id", evaluation.evaluationId,
                "film_id", evaluation.filmId,
                "film_title", evaluation.filmTitle,
                "score", evaluation.score,
                "remarks", evaluation.remarks,
                "created_at", toIsoString(evaluation.createdAt)
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void handleUser(HttpExchange exchange, String action,
                                   AuthorizationFilter.AuthResult auth) throws IOException, SQLException {
        if (!ensureAuthenticated(exchange, auth)) {
            return;
        }

        String method = exchange.getRequestMethod();

        if ("POST".equals(method) && "book-ticket".equals(action)) {
            bookTicket(exchange, auth.userId);
        } else if ("GET".equals(method) && "my-bookings".equals(action)) {
            getMyBookings(exchange, auth.userId);
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
        }
    }

    private static void bookTicket(HttpExchange exchange, Integer userId) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));

        Integer screeningId = parseInt(body.get("screening_id"), null);
        String seatNumber = body.get("seat_number");

        if (screeningId == null) {
            sendJson(exchange, HttpURLConnection.HTTP_BAD_REQUEST, errorJson("Missing screening_id"));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Get attendee_id for user
            int attendeeId;
            String sqlA = "SELECT attendee_id FROM attendee WHERE user_id = ?";
            PreparedStatement psA = con.prepareStatement(sqlA);
            psA.setInt(1, userId);
            try (ResultSet rsA = psA.executeQuery()) {
                if (rsA.next()) attendeeId = rsA.getInt(1);
                else {
                    // Create attendee record if missing
                    String sqlC = "INSERT INTO attendee (user_id) VALUES (?)";
                    PreparedStatement psC = con.prepareStatement(sqlC, Statement.RETURN_GENERATED_KEYS);
                    psC.setInt(1, userId);
                    psC.executeUpdate();
                    try (ResultSet rsC = psC.getGeneratedKeys()) {
                        rsC.next();
                        attendeeId = rsC.getInt(1);
                    }
                }
            }

            // Get price from screening
            double price = 0;
            String sqlS = "SELECT ticket_price FROM screening WHERE screening_id = ?";
            PreparedStatement psS = con.prepareStatement(sqlS);
            psS.setInt(1, screeningId);
            try (ResultSet rsS = psS.executeQuery()) {
                if (rsS.next()) price = rsS.getDouble(1);
            }

            // Create ticket
            String sqlT = "INSERT INTO ticket (screening_id, attendee_id, seat_number, price) VALUES (?, ?, ?, ?)";
            PreparedStatement psBook = con.prepareStatement(sqlT, Statement.RETURN_GENERATED_KEYS);
            psBook.setInt(1, screeningId);
            psBook.setInt(2, attendeeId);
            psBook.setString(3, seatNumber != null ? seatNumber : "ANY");
            psBook.setDouble(4, price);
            psBook.executeUpdate();
            
            try (ResultSet rsT = psBook.getGeneratedKeys()) {
                if (rsT.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_CREATED, toJsonObject(mapOf("success", true, "ticket_id", rsT.getInt(1))));
                }
            }
        }
    }

    private static void getMyBookings(HttpExchange exchange, Integer userId) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT t.ticket_id, t.screening_id, s.screening_date, " +
                "f.title, 1 AS seats_count, t.price AS total_price, t.seat_number " +
                "FROM ticket t " +
                "JOIN screening s ON t.screening_id = s.screening_id " +
                "JOIN film f ON s.film_id = f.film_id " +
                "JOIN attendee a ON t.attendee_id = a.attendee_id " +
                "WHERE a.user_id = ? " +
                "ORDER BY s.screening_date DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            List<Map<String, Object>> bookings = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapOf(
                        "ticket_id", rs.getInt("ticket_id"),
                        "screening_id", rs.getInt("screening_id"),
                        "film_title", rs.getString("title"),
                        "screening_date", rs.getString("screening_date"),
                        "seats_count", rs.getInt("seats_count"),
                        "total_price", rs.getDouble("total_price"),
                        "seat_number", rs.getString("seat_number")
                    ));
                }
            }

            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(bookings));
        }
    }

        private static void handleFilms(HttpExchange exchange, String subResource,
                                    AuthorizationFilter.AuthResult auth) throws IOException, SQLException {
        String method = exchange.getRequestMethod();

        if ("GET".equals(method)) {
            if ("leaderboard".equals(subResource)) {
                getTopRatedFilms(exchange);
            } else if (subResource != null && subResource.matches("\\d+")) {
                getFilmById(exchange, Integer.parseInt(subResource));
            } else {
                listAllFilms(exchange);
            }
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createFilm(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateFilm(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteFilm(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllFilms(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getQuery();
        String genre = null;
        String language = null;
        String sortBy = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length != 2) {
                    continue;
                }

                String key = decodeQueryValue(kv[0]);
                String value = decodeQueryValue(kv[1]);
                if ("genre".equals(key)) {
                    genre = value;
                } else if ("language".equals(key)) {
                    language = value;
                } else if ("sort".equals(key)) {
                    sortBy = value;
                }
            }
        }

        List<FilmService.FilmDTO> films = FilmService.getFilms(genre, language, sortBy);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FilmService.FilmDTO film : films) {
            result.add(mapOf(
                "film_id", film.filmId,
                "title", film.title,
                "genre", film.genre,
                "language", film.language,
                "runtime", film.runtime,
                "avg_score", film.averageScore,
                "evaluation_count", film.evaluationCount
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void getFilmById(HttpExchange exchange, int id) throws IOException, SQLException {
        FilmService.FilmDTO film = FilmService.getFilmById(id);
        if (film != null) {
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                "film_id", film.filmId,
                "title", film.title,
                "genre", film.genre,
                "language", film.language,
                "runtime", film.runtime
            )));
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film not found"));
        }
    }

    private static void getTopRatedFilms(HttpExchange exchange) throws IOException, SQLException {
        List<FilmService.FilmDTO> films = FilmService.getTopRatedFilms(10);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FilmService.FilmDTO film : films) {
            result.add(mapOf(
                "film_id", film.filmId,
                "title", film.title,
                "genre", film.genre,
                "language", film.language,
                "avg_score", film.averageScore,
                "evaluation_count", film.evaluationCount
            ));
        }

        sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(result));
    }

    private static void createFilm(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        
        FilmService.FilmDTO film = FilmService.createFilm(
            body.get("title"),
            body.get("director"),
            body.get("genre"),
            body.get("description"),
            parseInt(body.get("runtime"), parseInt(body.get("duration_minutes"), 0)),
            parseInt(body.get("release_year"), 2024),
            body.get("country"),
            body.get("language")
        );
        
        sendJson(exchange, HttpURLConnection.HTTP_CREATED, toJsonObject(mapOf(
            "film_id", film.filmId,
            "title", film.title,
            "genre", film.genre,
            "language", film.language,
            "runtime", film.runtime
        )));
    }

    private static void updateFilm(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        
        FilmService.FilmDTO film = FilmService.updateFilm(
            id,
            body.get("title"),
            body.get("director"),
            body.get("genre"),
            body.get("description"),
            parseInt(body.get("runtime"), parseInt(body.get("duration_minutes"), 0)),
            parseInt(body.get("release_year"), 2024),
            body.get("country"),
            body.get("language")
        );
        
        if (film != null) {
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                "film_id", film.filmId,
                "title", film.title,
                "success", true
            )));
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film not found"));
        }
    }

    private static void deleteFilm(HttpExchange exchange, int id) throws IOException, SQLException {
        boolean success = FilmService.deleteFilm(id);
        if (success) {
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
        } else {
            sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film not found"));
        }
    }

    private static void handleAttendees(HttpExchange exchange, String subResource) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getAttendeeById(exchange, Integer.parseInt(subResource));
            else listAllAttendees(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createAttendee(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateAttendee(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteAttendee(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllAttendees(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT a.*, u.name, u.email FROM attendee a JOIN users u ON a.user_id = u.user_id";
            PreparedStatement ps = con.prepareStatement(sql);
            List<Map<String, Object>> attendees = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    attendees.add(mapOf(
                        "attendee_id", rs.getInt("attendee_id"),
                        "user_id", rs.getInt("user_id"),
                        "name", rs.getString("name"),
                        "email", rs.getString("email")
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(attendees));
        }
    }

    private static void getAttendeeById(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT a.*, u.name, u.email FROM attendee a JOIN users u ON a.user_id = u.user_id WHERE attendee_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "attendee_id", rs.getInt("attendee_id"),
                        "user_id", rs.getInt("user_id"),
                        "name", rs.getString("name"),
                        "email", rs.getString("email")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Attendee not found"));
            }
        }
    }

    private static void createAttendee(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO attendee (user_id) VALUES (?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, parseInt(body.get("user_id"), 0));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) getAttendeeById(exchange, rs.getInt(1));
            }
        }
    }

    private static void updateAttendee(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE attendee SET user_id=? WHERE attendee_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, parseInt(body.get("user_id"), 0));
            ps.setInt(2, id);
            if (ps.executeUpdate() > 0) getAttendeeById(exchange, id);
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Attendee not found"));
        }
    }

    private static void deleteAttendee(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM attendee WHERE attendee_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Attendee not found"));
        }
    }

    private static void handleAwards(HttpExchange exchange, String subResource) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getAwardById(exchange, Integer.parseInt(subResource));
            else listAllAwards(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createAward(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateAward(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteAward(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllAwards(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM awards";
            PreparedStatement ps = con.prepareStatement(sql);
            List<Map<String, Object>> awards = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    awards.add(mapOf(
                        "award_id", rs.getInt("award_id"),
                        "award_name", rs.getString("award_name"),
                        "film_id", rs.getObject("film_id"),
                        "crew_id", rs.getObject("crew_id"),
                        "year", rs.getInt("year")
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(awards));
        }
    }

    private static void getAwardById(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM awards WHERE award_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "award_id", rs.getInt("award_id"),
                        "award_name", rs.getString("award_name"),
                        "film_id", rs.getObject("film_id"),
                        "crew_id", rs.getObject("crew_id"),
                        "year", rs.getInt("year")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Award not found"));
            }
        }
    }

    private static void createAward(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO awards (award_name, film_id, crew_id, year) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, body.get("award_name"));
            
            Integer filmId = parseInt(body.get("film_id"), null);
            if (filmId != null) ps.setInt(2, filmId); else ps.setNull(2, java.sql.Types.INTEGER);
            
            Integer crewId = parseInt(body.get("crew_id"), null);
            if (crewId != null) ps.setInt(3, crewId); else ps.setNull(3, java.sql.Types.INTEGER);
            
            ps.setInt(4, parseInt(body.get("year"), 2024));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) getAwardById(exchange, rs.getInt(1));
            }
        }
    }

    private static void updateAward(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE awards SET award_name=?, film_id=?, crew_id=?, year=? WHERE award_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, body.get("award_name"));
            
            Integer filmId = parseInt(body.get("film_id"), null);
            if (filmId != null) ps.setInt(2, filmId); else ps.setNull(2, java.sql.Types.INTEGER);
            
            Integer crewId = parseInt(body.get("crew_id"), null);
            if (crewId != null) ps.setInt(3, crewId); else ps.setNull(3, java.sql.Types.INTEGER);
            
            ps.setInt(4, parseInt(body.get("year"), 2024));
            ps.setInt(5, id);
            if (ps.executeUpdate() > 0) getAwardById(exchange, id);
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Award not found"));
        }
    }

    private static void deleteAward(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM awards WHERE award_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Award not found"));
        }
    }

    private static void handleVenues(HttpExchange exchange, String subResource) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getVenueById(exchange, Integer.parseInt(subResource));
            else listAllVenues(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createVenue(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateVenue(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteVenue(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllVenues(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM venue";
            PreparedStatement ps = con.prepareStatement(sql);
            List<Map<String, Object>> venues = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    venues.add(mapOf(
                        "venue_id", rs.getInt("venue_id"),
                        "name", rs.getString("name"),
                        "location", rs.getString("location"),
                        "capacity", rs.getInt("capacity")
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(venues));
        }
    }

    private static void getVenueById(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM venue WHERE venue_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "venue_id", rs.getInt("venue_id"),
                        "name", rs.getString("name"),
                        "location", rs.getString("location"),
                        "capacity", rs.getInt("capacity")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Venue not found"));
            }
        }
    }

    private static void createVenue(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO venue (name, location, capacity) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, body.get("name"));
            ps.setString(2, body.get("location"));
            ps.setInt(3, parseInt(body.get("capacity"), 0));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) getVenueById(exchange, rs.getInt(1));
            }
        }
    }

    private static void updateVenue(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE venue SET name=?, location=?, capacity=? WHERE venue_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, body.get("name"));
            ps.setString(2, body.get("location"));
            ps.setInt(3, parseInt(body.get("capacity"), 0));
            ps.setInt(4, id);
            if (ps.executeUpdate() > 0) getVenueById(exchange, id);
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Venue not found"));
        }
    }

    private static void deleteVenue(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM venue WHERE venue_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Venue not found"));
        }
    }

    private static void handleScreenings(HttpExchange exchange, String subResource,
                                         AuthorizationFilter.AuthResult auth) throws IOException, SQLException {
        String method = exchange.getRequestMethod();

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getScreeningById(exchange, Integer.parseInt(subResource));
            else listAllScreenings(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createScreening(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateScreening(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteScreening(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllScreenings(HttpExchange exchange) throws IOException, SQLException {
        // Parse optional film_id query param
        String query = exchange.getRequestURI().getQuery();
        Integer filmIdFilter = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "film_id".equals(decodeQueryValue(kv[0]))) {
                    filmIdFilter = parseInt(decodeQueryValue(kv[1]), null);
                }
            }
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT s.*, f.title as film_title, v.name as venue_name FROM screening s " +
                         "JOIN film f ON s.film_id = f.film_id JOIN venue v ON s.venue_id = v.venue_id" +
                         (filmIdFilter != null ? " WHERE s.film_id = ?" : "") +
                         " ORDER BY s.screening_date ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            if (filmIdFilter != null) ps.setInt(1, filmIdFilter);
            List<Map<String, Object>> screenings = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    screenings.add(mapOf(
                        "screening_id", rs.getInt("screening_id"),
                        "film_id", rs.getInt("film_id"),
                        "film_title", rs.getString("film_title"),
                        "venue_id", rs.getInt("venue_id"),
                        "venue_name", rs.getString("venue_name"),
                        "screening_date", rs.getDate("screening_date").toString(),
                        "start_time", rs.getTime("start_time").toString(),
                        "end_time", rs.getTime("end_time").toString(),
                        "ticket_price", rs.getBigDecimal("ticket_price")
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(screenings));
        }
    }

    private static void getScreeningById(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT s.*, f.title as film_title, v.name as venue_name FROM screening s " +
                         "JOIN film f ON s.film_id = f.film_id JOIN venue v ON s.venue_id = v.venue_id WHERE screening_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "screening_id", rs.getInt("screening_id"),
                        "film_id", rs.getInt("film_id"),
                        "film_title", rs.getString("film_title"),
                        "venue_id", rs.getInt("venue_id"),
                        "venue_name", rs.getString("venue_name"),
                        "screening_date", rs.getDate("screening_date").toString(),
                        "start_time", rs.getTime("start_time").toString(),
                        "end_time", rs.getTime("end_time").toString(),
                        "ticket_price", rs.getBigDecimal("ticket_price")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Screening not found"));
            }
        }
    }

    private static void createScreening(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO screening (film_id, venue_id, screening_date, start_time, end_time, ticket_price) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, parseInt(body.get("film_id"), 0));
            ps.setInt(2, parseInt(body.get("venue_id"), 0));
            ps.setDate(3, java.sql.Date.valueOf(body.get("screening_date")));
            ps.setTime(4, java.sql.Time.valueOf(body.get("start_time")));
            ps.setTime(5, java.sql.Time.valueOf(body.get("end_time")));
            ps.setBigDecimal(6, new java.math.BigDecimal(body.get("ticket_price")));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) { int newId = rs.getInt(1); getScreeningById(exchange, newId); }
            }
        }
    }

    private static void updateScreening(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE screening SET film_id=?, venue_id=?, screening_date=?, start_time=?, end_time=?, ticket_price=? WHERE screening_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, parseInt(body.get("film_id"), 0));
            ps.setInt(2, parseInt(body.get("venue_id"), 0));
            ps.setDate(3, java.sql.Date.valueOf(body.get("screening_date")));
            ps.setTime(4, java.sql.Time.valueOf(body.get("start_time")));
            ps.setTime(5, java.sql.Time.valueOf(body.get("end_time")));
            ps.setBigDecimal(6, new java.math.BigDecimal(body.get("ticket_price")));
            ps.setInt(7, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Screening not found"));
        }
    }

    private static void deleteScreening(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM screening WHERE screening_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Screening not found"));
        }
    }

    private static void handleTickets(HttpExchange exchange, String subResource) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getTicketById_Admin(exchange, Integer.parseInt(subResource));
            else listAllTickets(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createTicket(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateTicket(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteTicket(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllTickets(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT t.*, s.screening_date, f.title as film_title, a.user_id " +
                         "FROM ticket t JOIN screening s ON t.screening_id = s.screening_id " +
                         "JOIN film f ON s.film_id = f.film_id JOIN attendee a ON t.attendee_id = a.attendee_id";
            PreparedStatement ps = con.prepareStatement(sql);
            List<Map<String, Object>> tickets = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapOf(
                        "ticket_id", rs.getInt("ticket_id"),
                        "screening_id", rs.getInt("screening_id"),
                        "attendee_id", rs.getInt("attendee_id"),
                        "seat_number", rs.getString("seat_number"),
                        "price", rs.getBigDecimal("price"),
                        "film_title", rs.getString("film_title"),
                        "screening_date", rs.getDate("screening_date").toString()
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(tickets));
        }
    }

    private static void getTicketById_Admin(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT t.*, f.title as film_title FROM ticket t " +
                         "JOIN screening s ON t.screening_id = s.screening_id " +
                         "JOIN film f ON s.film_id = f.film_id WHERE ticket_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "ticket_id", rs.getInt("ticket_id"),
                        "screening_id", rs.getInt("screening_id"),
                        "attendee_id", rs.getInt("attendee_id"),
                        "seat_number", rs.getString("seat_number"),
                        "price", rs.getBigDecimal("price"),
                        "film_title", rs.getString("film_title")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Ticket not found"));
            }
        }
    }

    private static void createTicket(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO ticket (screening_id, attendee_id, seat_number, price) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, parseInt(body.get("screening_id"), 0));
            ps.setInt(2, parseInt(body.get("attendee_id"), 0));
            ps.setString(3, body.get("seat_number"));
            ps.setBigDecimal(4, new java.math.BigDecimal(body.get("price")));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) getTicketById_Admin(exchange, rs.getInt(1));
            }
        }
    }

    private static void updateTicket(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE ticket SET screening_id=?, attendee_id=?, seat_number=?, price=? WHERE ticket_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, parseInt(body.get("screening_id"), 0));
            ps.setInt(2, parseInt(body.get("attendee_id"), 0));
            ps.setString(3, body.get("seat_number"));
            ps.setBigDecimal(4, new java.math.BigDecimal(body.get("price")));
            ps.setInt(5, id);
            if (ps.executeUpdate() > 0) getTicketById_Admin(exchange, id);
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Ticket not found"));
        }
    }

    private static void deleteTicket(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM ticket WHERE ticket_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Ticket not found"));
        }
    }

    private static void handleFilmCrew(HttpExchange exchange, String subResource) throws IOException, SQLException {
        String method = exchange.getRequestMethod();
        AuthorizationFilter.AuthResult auth = AuthorizationFilter.authenticate(exchange);

        if ("GET".equals(method)) {
            if (subResource != null && subResource.matches("\\d+")) getFilmCrewById(exchange, Integer.parseInt(subResource));
            else listAllFilmCrew(exchange);
            return;
        }

        if (!ensureAuthenticated(exchange, auth) || !AuthorizationFilter.hasRole(auth, "ADMIN")) {
            sendJson(exchange, HttpURLConnection.HTTP_FORBIDDEN, errorJson("Admin access required"));
            return;
        }

        if ("POST".equals(method)) createFilmCrew(exchange);
        else if ("PUT".equals(method) && subResource != null && subResource.matches("\\d+")) updateFilmCrew(exchange, Integer.parseInt(subResource));
        else if ("DELETE".equals(method) && subResource != null && subResource.matches("\\d+")) deleteFilmCrew(exchange, Integer.parseInt(subResource));
        else sendJson(exchange, HttpURLConnection.HTTP_BAD_METHOD, errorJson("Method not allowed"));
    }

    private static void listAllFilmCrew(HttpExchange exchange) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM film_crew";
            PreparedStatement ps = con.prepareStatement(sql);
            List<Map<String, Object>> crew = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    crew.add(mapOf(
                        "crew_id", rs.getInt("crew_id"),
                        "name", rs.getString("name"),
                        "role", rs.getString("role"),
                        "film_id", rs.getInt("film_id")
                    ));
                }
            }
            sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonArray(crew));
        }
    }

    private static void getFilmCrewById(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM film_crew WHERE crew_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf(
                        "crew_id", rs.getInt("crew_id"),
                        "name", rs.getString("name"),
                        "role", rs.getString("role"),
                        "film_id", rs.getInt("film_id")
                    )));
                } else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film crew member not found"));
            }
        }
    }

    private static void createFilmCrew(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO film_crew (name, role, film_id) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, body.get("name"));
            ps.setString(2, body.get("role"));
            ps.setInt(3, parseInt(body.get("film_id"), 0));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) getFilmCrewById(exchange, rs.getInt(1));
            }
        }
    }

    private static void updateFilmCrew(HttpExchange exchange, int id) throws IOException, SQLException {
        Map<String, String> body = parseJson(readRequestBody(exchange));
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE film_crew SET name=?, role=?, film_id=? WHERE crew_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, body.get("name"));
            ps.setString(2, body.get("role"));
            ps.setInt(3, parseInt(body.get("film_id"), 0));
            ps.setInt(4, id);
            if (ps.executeUpdate() > 0) getFilmCrewById(exchange, id);
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film crew member not found"));
        }
    }

    private static void deleteFilmCrew(HttpExchange exchange, int id) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM film_crew WHERE crew_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            if (ps.executeUpdate() > 0) sendJson(exchange, HttpURLConnection.HTTP_OK, toJsonObject(mapOf("success", true)));
            else sendJson(exchange, HttpURLConnection.HTTP_NOT_FOUND, errorJson("Film crew member not found"));
        }
    }

    private static boolean ensureAuthenticated(HttpExchange exchange,
                                               AuthorizationFilter.AuthResult auth) throws IOException {
        if (auth.isAuthenticated) {
            return true;
        }

        sendJson(exchange, HttpURLConnection.HTTP_UNAUTHORIZED,
            errorJson(auth.error != null ? auth.error : "Authentication required"));
        return false;
    }

    private static Integer getJuryIdForUser(Integer userId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT jury_id FROM jury WHERE user_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("jury_id");
                }
            }
        }
        return null;
    }

    private static void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] parts = json.split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String val = kv[1].trim().replace("\"", "");
                    result.put(key, val);
                }
            }
        }
        return result;
    }

    private static List<Integer> parseIntegerList(String json) {
        List<Integer> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        json = json.trim().replace("[", "").replace("]", "");
        for (String part : json.split(",")) {
            String val = part.trim();
            if (!val.isEmpty()) result.add(Integer.parseInt(val));
        }
        return result;
    }

    private static String decodeQueryValue(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String toIsoString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String toJsonObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");

            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                sb.append(toJsonObject(nestedMap));
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }

            if (iterator.hasNext()) {
                sb.append(",");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJsonObject(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String errorJson(String message) {
        return toJsonObject(mapOf(
            "success", false,
            "error", message
        ));
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private static Integer parseInt(String value, Integer defaultValue) {
        try {
            return value != null ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
