package auth;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT Token Provider - Handles token generation and validation
 * Uses HMAC-SHA256 without external dependencies
 */
public class JwtTokenProvider {
    private static final String SECRET_KEY = getSecretKey();
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours

    private static String getSecretKey() {
        String env = System.getenv("JWT_SECRET");
        return env != null && !env.isBlank() ? env : 
            "film-festival-secret-key-change-in-production-min-32-chars-long!";
    }

    /**
     * Generate JWT token for authenticated user
     */
    public static String generateToken(int userId, String email, String role) {
        Map<String, Object> payload = new LinkedHashMap<>();
        long now = System.currentTimeMillis() / 1000;
        
        payload.put("user_id", userId);
        payload.put("email", email);
        payload.put("role", role);
        payload.put("iat", now);
        payload.put("exp", now + (EXPIRATION_TIME / 1000));
        
        return createJwt(payload);
    }

    /**
     * Validate JWT token and return payload
     */
    public static Map<String, Object> validateToken(String token) throws Exception {
        if (token == null || token.isBlank()) {
            throw new Exception("Token is empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new Exception("Invalid token format");
        }

        // Verify signature
        String expectedSignature = createSignature(parts[0] + "." + parts[1]);
        if (!expectedSignature.equals(parts[2])) {
            throw new Exception("Invalid token signature");
        }

        // Decode payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), 
            StandardCharsets.UTF_8);
        Map<String, Object> payload = jsonToMap(payloadJson);

        // Check expiration
        Long exp = extractLong(payload.get("exp"));
        if (exp != null && exp < System.currentTimeMillis() / 1000) {
            throw new Exception("Token expired");
        }

        return payload;
    }

    /**
     * Create JWT with header, payload, and signature
     */
    private static String createJwt(Map<String, Object> payload) {
        String header = encodeBase64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payloadJson = mapToJson(payload);
        String encodedPayload = encodeBase64(payloadJson);
        
        String message = header + "." + encodedPayload;
        String signature = createSignature(message);
        
        return message + "." + signature;
    }

    /**
     * Create HMAC-SHA256 signature
     */
    private static String createSignature(String message) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec spec = 
                new javax.crypto.spec.SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return encodeBase64(signature);
        } catch (Exception e) {
            throw new RuntimeException("Signature creation failed", e);
        }
    }

    /**
     * Base64 URL-encode bytes
     */
    private static String encodeBase64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * Base64 URL-encode string
     */
    private static String encodeBase64(String data) {
        return encodeBase64(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert map to JSON string
     */
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert JSON string to map
     */
    private static Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();
        
        // Remove outer braces
        json = json.substring(1, json.length() - 1);
        
        if (json.isBlank()) {
            return map;
        }
        
        int depth = 0;
        StringBuilder current = new StringBuilder();
        
        for (char c : json.toCharArray()) {
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parsePair(current.toString(), map);
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        
        if (current.length() > 0) {
            parsePair(current.toString(), map);
        }
        
        return map;
    }

    /**
     * Parse key:value pair
     */
    private static void parsePair(String pair, Map<String, Object> map) {
        pair = pair.trim();
        int colonIdx = pair.indexOf(':');
        
        if (colonIdx == -1) return;
        
        String key = pair.substring(0, colonIdx).replaceAll("[\"\\s]", "");
        String valueStr = pair.substring(colonIdx + 1).trim();
        
        Object value;
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            value = valueStr.substring(1, valueStr.length() - 1);
        } else if ("null".equals(valueStr)) {
            value = null;
        } else if ("true".equals(valueStr)) {
            value = true;
        } else if ("false".equals(valueStr)) {
            value = false;
        } else {
            try {
                if (valueStr.contains(".")) {
                    value = Double.parseDouble(valueStr);
                } else {
                    value = Long.parseLong(valueStr);
                }
            } catch (NumberFormatException e) {
                value = valueStr;
            }
        }
        
        map.put(key, value);
    }

    /**
     * Extract Long from Object safely
     */
    private static Long extractLong(Object obj) {
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Double) return ((Double) obj).longValue();
        return null;
    }
}
