package film;

import db.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * FilmService - Handles film operations and retrieval
 */
public class FilmService {
    
    public static class FilmDTO {
        public Integer filmId;
        public String title;
        public Integer runtime;
        public String language;
        public String genre;
        public Double averageScore;
        public Integer evaluationCount;

        public FilmDTO(Integer filmId, String title, Integer runtime, 
                      String language, String genre) {
            this.filmId = filmId;
            this.title = title;
            this.runtime = runtime;
            this.language = language;
            this.genre = genre;
            this.averageScore = null;
            this.evaluationCount = 0;
        }
    }

    /**
     * Get all films with optional filtering
     */
    public static List<FilmDTO> getFilms(String genre, String language, 
                                         String sortBy) throws SQLException {
        List<FilmDTO> films = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT f.film_id, f.title, f.runtime, f.language, f.genre, " +
            "AVG(e.score) as avg_score, COUNT(e.evaluation_id) as eval_count " +
            "FROM film f " +
            "LEFT JOIN evaluation e ON f.film_id = e.film_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        // Apply filters
        if (genre != null && !genre.isBlank()) {
            sql.append("AND f.genre = ? ");
            params.add(genre);
        }

        if (language != null && !language.isBlank()) {
            sql.append("AND f.language = ? ");
            params.add(language);
        }

        sql.append("GROUP BY f.film_id ");

        // Apply sorting
        if ("rating".equals(sortBy)) {
            sql.append("ORDER BY avg_score DESC ");
        } else if ("title".equals(sortBy)) {
            sql.append("ORDER BY f.title ASC ");
        } else {
            sql.append("ORDER BY f.film_id DESC ");
        }

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilmDTO film = new FilmDTO(
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("runtime"),
                        rs.getString("language"),
                        rs.getString("genre")
                    );
                    
                    double avgScore = rs.getDouble("avg_score");
                    film.averageScore = rs.wasNull() ? null : avgScore;
                    film.evaluationCount = rs.getInt("eval_count");
                    
                    films.add(film);
                }
            }
        }

        return films;
    }

    /**
     * Get single film by ID
     */
    public static FilmDTO getFilmById(Integer filmId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT f.film_id, f.title, f.runtime, f.language, f.genre, " +
                        "AVG(e.score) as avg_score, COUNT(e.evaluation_id) as eval_count " +
                        "FROM film f " +
                        "LEFT JOIN evaluation e ON f.film_id = e.film_id " +
                        "WHERE f.film_id = ? " +
                        "GROUP BY f.film_id";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    FilmDTO film = new FilmDTO(
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("runtime"),
                        rs.getString("language"),
                        rs.getString("genre")
                    );
                    
                    double avgScore = rs.getDouble("avg_score");
                    film.averageScore = rs.wasNull() ? null : avgScore;
                    film.evaluationCount = rs.getInt("eval_count");
                    
                    return film;
                }
            }
        }

        return null;
    }

    /**
     * Get top rated films (leaderboard)
     */
    public static List<FilmDTO> getTopRatedFilms(Integer limit) throws SQLException {
        List<FilmDTO> films = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT f.film_id, f.title, f.runtime, f.language, f.genre, " +
                        "AVG(e.score) as avg_score, COUNT(e.evaluation_id) as eval_count " +
                        "FROM film f " +
                        "JOIN evaluation e ON f.film_id = e.film_id " +
                        "GROUP BY f.film_id " +
                        "ORDER BY avg_score DESC " +
                        "LIMIT ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, limit != null ? limit : 10);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilmDTO film = new FilmDTO(
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("runtime"),
                        rs.getString("language"),
                        rs.getString("genre")
                    );
                    
                    double avgScore = rs.getDouble("avg_score");
                    film.averageScore = rs.wasNull() ? null : avgScore;
                    film.evaluationCount = rs.getInt("eval_count");
                    
                    films.add(film);
                }
            }
        }

        return films;
    }

    /**
     * Get award-eligible films (avg score > 7.5)
     */
    public static List<FilmDTO> getAwardEligibleFilms() throws SQLException {
        List<FilmDTO> films = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT f.film_id, f.title, f.runtime, f.language, f.genre, " +
                        "AVG(e.score) as avg_score, COUNT(e.evaluation_id) as eval_count " +
                        "FROM film f " +
                        "JOIN evaluation e ON f.film_id = e.film_id " +
                        "GROUP BY f.film_id " +
                        "HAVING AVG(e.score) >= 7.5 " +
                        "ORDER BY avg_score DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FilmDTO film = new FilmDTO(
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("runtime"),
                        rs.getString("language"),
                        rs.getString("genre")
                    );
                    
                    double avgScore = rs.getDouble("avg_score");
                    film.averageScore = rs.wasNull() ? null : avgScore;
                    film.evaluationCount = rs.getInt("eval_count");
                    
                    films.add(film);
                }
            }
        }

        return films;
    }
}
