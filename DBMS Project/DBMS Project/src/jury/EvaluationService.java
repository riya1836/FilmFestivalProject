package jury;

import db.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * EvaluationService - Handles film evaluations by jury members
 */
public class EvaluationService {
    
    public static class EvaluationDTO {
        public Integer evaluationId;
        public Integer juryId;
        public Integer filmId;
        public String filmTitle;
        public Integer score;
        public String remarks;
        public Timestamp createdAt;

        public EvaluationDTO(Integer evaluationId, Integer juryId, Integer filmId,
                            String filmTitle, Integer score, String remarks, 
                            Timestamp createdAt) {
            this.evaluationId = evaluationId;
            this.juryId = juryId;
            this.filmId = filmId;
            this.filmTitle = filmTitle;
            this.score = score;
            this.remarks = remarks;
            this.createdAt = createdAt;
        }
    }

    /**
     * Submit evaluation for a film
     */
    public static EvaluationDTO submitEvaluation(Integer juryId, Integer filmId, 
                                                  Integer score, String remarks) 
            throws SQLException {
        // Validate score
        if (score == null || score < 1 || score > 10) {
            throw new SQLException("Score must be between 1 and 10");
        }

        // Validate remarks
        if (remarks != null && remarks.length() > 1000) {
            throw new SQLException("Remarks cannot exceed 1000 characters");
        }

        // Check if already evaluated
        if (evaluationExists(juryId, filmId)) {
            throw new SQLException("You have already evaluated this film");
        }

        // Check if jury is assigned to this film
        if (!isAssigned(juryId, filmId)) {
            throw new SQLException("You are not assigned to evaluate this film");
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO evaluation (jury_id, film_id, score, remarks) " +
                        "VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, juryId);
            ps.setInt(2, filmId);
            ps.setInt(3, score);
            if (remarks != null && !remarks.isBlank()) {
                ps.setString(4, remarks);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert evaluation");
            }
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Integer evaluationId = rs.getInt(1);
                    String filmTitle = getFilmTitle(filmId);
                    return new EvaluationDTO(evaluationId, juryId, filmId, 
                        filmTitle, score, remarks, new Timestamp(
                            System.currentTimeMillis()));
                }
            }
        }
        
        throw new SQLException("Failed to submit evaluation");
    }

    /**
     * Get all evaluations for a jury member
     */
    public static List<EvaluationDTO> getJuryEvaluations(Integer juryId) 
            throws SQLException {
        List<EvaluationDTO> evaluations = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT e.evaluation_id, e.jury_id, e.film_id, " +
                        "f.title, e.score, e.remarks, e.created_at " +
                        "FROM evaluation e " +
                        "JOIN film f ON e.film_id = f.film_id " +
                        "WHERE e.jury_id = ? " +
                        "ORDER BY e.created_at DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, juryId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(new EvaluationDTO(
                        rs.getInt("evaluation_id"),
                        rs.getInt("jury_id"),
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("score"),
                        rs.getString("remarks"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return evaluations;
    }

    /**
     * Get all evaluations for a specific film
     */
    public static List<EvaluationDTO> getFilmEvaluations(Integer filmId) 
            throws SQLException {
        List<EvaluationDTO> evaluations = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT e.evaluation_id, e.jury_id, e.film_id, " +
                        "f.title, e.score, e.remarks, e.created_at " +
                        "FROM evaluation e " +
                        "JOIN film f ON e.film_id = f.film_id " +
                        "WHERE e.film_id = ? " +
                        "ORDER BY e.score DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(new EvaluationDTO(
                        rs.getInt("evaluation_id"),
                        rs.getInt("jury_id"),
                        rs.getInt("film_id"),
                        rs.getString("title"),
                        rs.getInt("score"),
                        rs.getString("remarks"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return evaluations;
    }

    /**
     * Get average score for a film
     */
    public static Double getAverageScore(Integer filmId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT AVG(score) as avg_score FROM evaluation " +
                        "WHERE film_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avgScore = rs.getDouble("avg_score");
                    return rs.wasNull() ? null : avgScore;
                }
            }
        }
        return null;
    }

    /**
     * Get evaluation count for a film
     */
    public static Integer getEvaluationCount(Integer filmId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM evaluation " +
                        "WHERE film_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    /**
     * Check if jury has already evaluated film
     */
    private static boolean evaluationExists(Integer juryId, Integer filmId) 
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT 1 FROM evaluation " +
                        "WHERE jury_id = ? AND film_id = ? LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, juryId);
            ps.setInt(2, filmId);
            return ps.executeQuery().next();
        }
    }

    /**
     * Check if jury is assigned to evaluate film
     */
    private static boolean isAssigned(Integer juryId, Integer filmId) 
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
