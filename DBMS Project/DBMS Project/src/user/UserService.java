package user;

import auth.PasswordEncoder;
import db.DBConnection;
import java.sql.*;
import java.util.*;

/**
 * UserService - Handles user registration, login, and management
 */
public class UserService {
    
    public static class UserDTO {
        public Integer userId;
        public String name;
        public String email;
        public String role;
        public Boolean isActive;

        public UserDTO(Integer userId, String name, String email, 
                      String role, Boolean isActive) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            return "UserDTO{" +
                    "userId=" + userId +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", role='" + role + '\'' +
                    ", isActive=" + isActive +
                    '}';
        }
    }

    /**
     * Register a new user
     */
    public static UserDTO registerUser(String name, String email, 
                                       String password, String role) throws SQLException {
        // Validation
        if (name == null || name.isBlank()) {
            throw new SQLException("Name is required");
        }
        if (!isValidEmail(email)) {
            throw new SQLException("Invalid email format");
        }
        if (password == null || password.length() < 6) {
            throw new SQLException("Password must be at least 6 characters");
        }
        if (!isValidRole(role)) {
            throw new SQLException("Invalid role: " + role);
        }

        // Check if user exists
        if (userExists(email)) {
            throw new SQLException("Email already registered");
        }

        String passwordHash = PasswordEncoder.encode(password);

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (name, email, password_hash, role, is_active) " +
                        "VALUES (?, ?, ?, ?, true)";
            PreparedStatement ps = con.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert user");
            }
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Integer userId = rs.getInt(1);
                    
                    // If role is JURY, create jury record
                    if ("JURY".equals(role)) {
                        createJuryRecord(con, userId);
                    } else if ("USER".equals(role)) {
                        createAttendeeRecord(con, userId, name, email);
                    }
                    
                    return new UserDTO(userId, name, email, role, true);
                }
            }
        }
        
        throw new SQLException("Failed to create user");
    }

    /**
     * Login user with email and password
     */
    public static UserDTO loginUser(String email, String password) 
            throws SQLException {
        if (!isValidEmail(email)) {
            throw new SQLException("Invalid email");
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT user_id, name, email, password_hash, role " +
                        "FROM users WHERE email = ? AND is_active = true";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordEncoder.matches(password, storedHash)) {
                        return new UserDTO(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            true
                        );
                    }
                }
            }
        }
        
        throw new SQLException("Invalid email or password");
    }

    /**
     * Get user by ID
     */
    public static UserDTO getUserById(Integer userId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT user_id, name, email, role, is_active " +
                        "FROM users WHERE user_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserDTO(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("is_active")
                    );
                }
            }
        }
        
        return null;
    }

    /**
     * Deactivate user account
     */
    public static void deactivateUser(Integer userId) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE users SET is_active = false WHERE user_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Create jury record for new jury user
     */
    private static void createJuryRecord(Connection con, Integer userId)
            throws SQLException {
        int juryId = nextId(con, "jury", "jury_id");
        String sql = "INSERT INTO jury (jury_id, user_id, is_active) VALUES (?, ?, true)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, juryId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Create attendee record for a new user account.
     */
    private static void createAttendeeRecord(Connection con, Integer userId, String name, String email)
            throws SQLException {
        int attendeeId = nextId(con, "attendee", "attendee_id");
        String[] nameParts = splitName(name);
        String sql = "INSERT INTO attendee " +
            "(attendee_id, first_name, last_name, email, phone, user_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, true)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            ps.setString(2, nameParts[0]);
            ps.setString(3, nameParts[1]);
            ps.setString(4, email);
            ps.setString(5, "");
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }

    private static int nextId(Connection con, String table, String idColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + idColumn + "), 0) + 1 AS next_id FROM " + table;
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        }
        return 1;
    }

    private static String[] splitName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) {
            return new String[] {"User", ""};
        }

        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return new String[] {normalized, ""};
        }

        String firstName = normalized.substring(0, separator).trim();
        String lastName = normalized.substring(separator + 1).trim();
        return new String[] {
            firstName.isEmpty() ? "User" : firstName,
            lastName
        };
    }

    /**
     * Check if email already exists
     */
    private static boolean userExists(String email) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT 1 FROM users WHERE email = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    /**
     * Validate email format
     */
    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validate role
     */
    private static boolean isValidRole(String role) {
        return role != null && (role.equals("ADMIN") || role.equals("USER") || role.equals("JURY"));
    }
}
