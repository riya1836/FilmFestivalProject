package app;

import db.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class RunMigration {
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE award ADD COLUMN crew_id INT NULL");
            System.out.println("[OK] Migration applied: award.crew_id added (nullable)");
        } catch (SQLException e) {
            System.err.println("[ERROR] Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
