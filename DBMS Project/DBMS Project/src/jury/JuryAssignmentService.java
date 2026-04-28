package jury;

import db.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * JuryAssignmentService - Handles jury assignments to films
 */
public class JuryAssignmentService {
    
    public static class JuryAssignmentDTO {
        public Integer assignmentId;
        public Integer juryId;
        public Integer filmId;
        public String filmTitle;
        public Timestamp assignedAt;

        public JuryAssignmentDTO(Integer assignmentId, Integer juryId, Integer filmId,
                                String filmTitle, Timestamp assignedAt) {
            this.assignmentId = assignmentId;
            this.juryId = juryId;
            this.filmId = filmId;
            this.filmTitle = filmTitle;
            this.assignedAt = assignedAt;
        }
    }

    /**
     * Assign jury to a film
     */
    public static JuryAssignmentDTO assignJuryToFilm(Integer juryId, Integer filmId) 
            throws SQLException {
        // Validate jury exists
        if (!juryExists(juryId)) {
            throw new SQLException("Jury member not found");
        }

        // Validate film exists
        if (!filmExists(filmId)) {
            throw new SQLException("Film not found");
        }

        // Check if already assigned
        if (isAlreadyAssigned(juryId, filmId)) {
            throw new SQLException("Jury already assigned to this film");
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO jury_assignment (jury_id, film_id) " +
                        "VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, juryId);
            ps.setInt(2, filmId);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to assign jury");
            }
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Integer assignmentId = rs.getInt(1);
                    String filmTitle = getFilmTitle(filmId);
                    return new JuryAssignmentDTO(assignmentId, juryId, filmId, 
                        filmTitle, new Timestamp(System.currentTimeMillis()));
                }
            }
        }
        
        throw new SQLException("Failed to create assignment");
    }

    /**
     * Assign jury to multiple films
     */
    public static List<JuryAssignmentDTO> assignJuryToMultipleFilms(Integer juryId, 
                                                                   List<Integer> filmIds) 
            throws SQLException {
        List<JuryAssignmentDTO> assignments = new ArrayList<>();
        
        for (Integer filmId : filmIds) {
            try {
                JuryAssignmentDTO assignment = assignJuryToFilm(juryId, filmId);
                assignments.add(assignment);
            } catch (SQLException e) {
                // Log but continue with other films
                System.err.println("Failed to assign film " + filmId + ": " + e.getMessage());
            }
        }
        
        return assignments;
    }

    /**
     * Remove jury assignment
     */
    public static void removeAssignment(Integer assignmentId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM jury_assignment WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, assignmentId);
            ps.executeUpdate();
        }
    }

    /**
     * Get all assignments for a jury member
     */
    public static List<JuryAssignmentDTO> getJuryAssignments(Integer juryId) 
            throws SQLException {
        List<JuryAssignmentDTO> assignments = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT ja.id, ja.jury_id, ja.film_id, f.title, ja.assigned_at " +
                        "FROM jury_assignment ja " +
                        "JOIN film f ON ja.film_id = f.film_id " +
                        "WHERE ja.jury_id = ? " +
                        "ORDER BY ja.assigned_at DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, juryId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new JuryAssignmentDTO(
                        rs.getInt("id"),
                        rs.getInt("jury_id"),
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getTimestamp("assigned_at")
                    ));
                }
            }
        }
        return assignments;
    }

    /**
     * Get all assignments for a film
     */
    public static List<JuryAssignmentDTO> getFilmAssignments(Integer filmId) 
            throws SQLException {
        List<JuryAssignmentDTO> assignments = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT ja.id, ja.jury_id, ja.film_id, f.title, ja.assigned_at " +
                        "FROM jury_assignment ja " +
                        "JOIN film f ON ja.film_id = f.film_id " +
                        "WHERE ja.film_id = ? " +
                        "ORDER BY ja.assigned_at DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new JuryAssignmentDTO(
                        rs.getInt("id"),
                        rs.getInt("jury_id"),
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getTimestamp("assigned_at")
                    ));
                }
            }
        }
        return assignments;
    }

    /**
     * Get all jury assignments
     */
    public static List<JuryAssignmentDTO> getAllAssignments() throws SQLException {
        List<JuryAssignmentDTO> assignments = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT ja.id, ja.jury_id, ja.film_id, f.title, ja.assigned_at " +
                        "FROM jury_assignment ja " +
                        "JOIN film f ON ja.film_id = f.film_id " +
                        "ORDER BY ja.assigned_at DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new JuryAssignmentDTO(
                        rs.getInt("id"),
                        rs.getInt("jury_id"),
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getTimestamp("assigned_at")
                    ));
                }
            }
        }
        return assignments;
    }

    /**
     * Check if jury is already assigned to film
     */
    private static boolean isAlreadyAssigned(Integer juryId, Integer filmId) 
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT 1 FROM jury_assignment " +
                        "WHERE jury_id = ? AND film_id = ? LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, juryId);
            ps.setInt(2, filmId);
            return ps.executeQuery().next();
        }
    }

    /**
     * Check if jury exists
     */
    private static boolean juryExists(Integer juryId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT 1 FROM jury WHERE jury_id = ? LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, juryId);
            return ps.executeQuery().next();
        }
    }

    /**
     * Check if film exists
     */
    private static boolean filmExists(Integer filmId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT 1 FROM film WHERE film_id = ? LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            return ps.executeQuery().next();
        }
    }

    /**
     * Get film title
     */
    private static String getFilmTitle(Integer filmId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT title FROM film WHERE film_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("title");
            }
        }
        return "Unknown Film";
    }
}
