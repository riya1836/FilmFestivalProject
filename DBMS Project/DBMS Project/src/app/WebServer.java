package app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import db.DBConnection;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

public class WebServer {
    private static final int PORT = 8080;
    private static final String FRONTEND_PATH = "frontend/dist";

    public static void start() {
        // Test database connection first
        testDatabaseConnection();
        
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            ApiServer.register(server);
            server.createContext("/", new StaticFileHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Web server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void testDatabaseConnection() {
        try {
            System.out.println("[TEST] Attempting database connection...");
            Connection con = DBConnection.getConnection();
            if (con != null && !con.isClosed()) {
                System.out.println("[SUCCESS] Database connection established successfully!");
                con.close();
            } else {
                System.out.println("[ERROR] Database connection is null or closed!");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to connect to database!");
            System.out.println("[ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path filePath = Paths.get(FRONTEND_PATH, requestPath.substring(1));
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(filePath.toString());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, Files.size(filePath));
                try (OutputStream os = exchange.getResponseBody();
                     InputStream is = Files.newInputStream(filePath)) {
                    is.transferTo(os);
                }
            } else {
                // Serve index.html for SPA routing
                Path indexPath = Paths.get(FRONTEND_PATH, "index.html");
                if (Files.exists(indexPath)) {
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, Files.size(indexPath));
                    try (OutputStream os = exchange.getResponseBody();
                         InputStream is = Files.newInputStream(indexPath)) {
                        is.transferTo(os);
                    }
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html";
            if (fileName.endsWith(".css")) return "text/css";
            if (fileName.endsWith(".js")) return "application/javascript";
            if (fileName.endsWith(".json")) return "application/json";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            return "text/plain";
        }
    }
}