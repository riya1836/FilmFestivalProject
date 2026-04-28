package db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/film_festival";
    private static final Properties FILE_PROPERTIES = loadFileProperties();

    public static Connection getConnection() throws SQLException {
        String url = resolveValue("FILM_FESTIVAL_DB_URL", "db.url", DEFAULT_URL);
        String user = resolveRequiredValue("FILM_FESTIVAL_DB_USER", "db.user");
        String password = resolveRequiredValue("FILM_FESTIVAL_DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadFileProperties() {
        Properties properties = new Properties();
        Path configPath = Path.of("db.properties");
        if (!Files.exists(configPath)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
            // The app can still fall back to environment variables or JVM properties.
        }

        return properties;
    }

    private static String resolveRequiredValue(String envKey, String propertyKey) throws SQLException {
        String value = resolveValue(envKey, propertyKey, null);
        if (value == null) {
            throw new SQLException(
                "Database configuration is missing. Set " + envKey + ", pass -D" + propertyKey
                    + "=..., or create db.properties from db.properties.example."
            );
        }
        return value;
    }

    private static String resolveValue(String envKey, String propertyKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        String systemProperty = System.getProperty(propertyKey);
        if (systemProperty != null) {
            return systemProperty;
        }

        String fileValue = FILE_PROPERTIES.getProperty(propertyKey);
        if (fileValue != null) {
            return fileValue;
        }

        return defaultValue;
    }
}

