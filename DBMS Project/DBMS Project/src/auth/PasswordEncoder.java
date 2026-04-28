package auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password Encoder - Handles password hashing and verification
 * Uses SHA-256 with salt and multiple iterations
 */
public class PasswordEncoder {
    private static final int HASH_ITERATIONS = 10000;
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "SHA-256";

    /**
     * Encode a plain text password
     * Returns: Base64(salt + hashed_password)
     */
    public static String encode(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            
            // Hash password with salt
            byte[] hashedPassword = hashPassword(password.getBytes(), salt);
            
            // Combine salt + hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, 
                hashedPassword.length);
            
            // Return Base64 encoded
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Password encoding failed", e);
        }
    }

    /**
     * Verify if plain password matches encoded hash
     */
    public static boolean matches(String plainPassword, String encodedPassword) {
        if (plainPassword == null || plainPassword.isBlank() || 
            encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }

        try {
            // Decode the encoded password
            byte[] combined = Base64.getDecoder().decode(encodedPassword);
            
            if (combined.length < SALT_LENGTH) {
                return false;
            }
            
            // Extract salt
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            
            // Hash the provided password with the same salt
            byte[] hashedPassword = hashPassword(plainPassword.getBytes(), salt);
            
            // Constant-time comparison (prevents timing attacks)
            return constantTimeEquals(combined, salt.length, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hash password with given salt
     */
    private static byte[] hashPassword(byte[] password, byte[] salt) 
            throws Exception {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        
        // Combine password and salt
        byte[] combined = new byte[password.length + salt.length];
        System.arraycopy(password, 0, combined, 0, password.length);
        System.arraycopy(salt, 0, combined, password.length, salt.length);
        
        byte[] result = md.digest(combined);
        
        // Hash multiple iterations
        for (int i = 1; i < HASH_ITERATIONS; i++) {
            md.reset();
            result = md.digest(result);
        }
        
        return result;
    }

    /**
     * Constant-time byte array comparison (prevents timing attacks)
     */
    private static boolean constantTimeEquals(byte[] combined, int offset, 
                                              byte[] expected) {
        int mismatch = 0;
        
        if (combined.length - offset != expected.length) {
            return false;
        }
        
        for (int i = 0; i < expected.length; i++) {
            mismatch |= (combined[offset + i] ^ expected[i]);
        }
        
        return mismatch == 0;
    }
}
