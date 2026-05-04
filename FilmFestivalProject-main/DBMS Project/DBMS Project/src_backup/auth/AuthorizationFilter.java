package auth;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;

/**
 * Authorization Filter - Handles JWT token validation and role checking
 */
public class AuthorizationFilter {
    
    public static class AuthResult {
        public boolean isAuthenticated;
        public Integer userId;
        public String email;
        public String role;
        public String error;

        public AuthResult(boolean authenticated, Integer userId, String email, 
                         String role, String error) {
            this.isAuthenticated = authenticated;
            this.userId = userId;
            this.email = email;
            this.role = role;
            this.error = error;
        }
    }

    /**
     * Authenticate request using Bearer token
     */
    public static AuthResult authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders()
            .getFirst("Authorization");
        
        if (authHeader == null || authHeader.isBlank()) {
            return new AuthResult(false, null, null, null, 
                "Missing authorization header");
        }

        if (!authHeader.startsWith("Bearer ")) {
            return new AuthResult(false, null, null, null, 
                "Invalid authorization format");
        }

        String token = authHeader.substring(7).trim();
        
        try {
            Map<String, Object> payload = JwtTokenProvider.validateToken(token);
            
            Integer userId = extractInteger(payload.get("user_id"));
            String email = (String) payload.get("email");
            String role = (String) payload.get("role");
            
            if (userId == null || email == null || role == null) {
                return new AuthResult(false, null, null, null, 
                    "Invalid token payload");
            }
            
            return new AuthResult(true, userId, email, role, null);
        } catch (Exception e) {
            return new AuthResult(false, null, null, null, 
                "Invalid or expired token: " + e.getMessage());
        }
    }

    /**
     * Check if user has required role
     */
    public static boolean hasRole(AuthResult auth, String... requiredRoles) {
        if (!auth.isAuthenticated || auth.role == null) {
            return false;
        }
        
        for (String role : requiredRoles) {
            if (auth.role.equals(role)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Extract Integer from Object safely
     */
    private static Integer extractInteger(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof Double) return ((Double) obj).intValue();
        return null;
    }
}
