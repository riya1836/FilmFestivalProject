package app;

import db.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MakeFilmNullable {
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE award MODIFY film_id INT NULL");
            System.out.println("[OK] Migration applied: award.film_id is now NULLABLE");
        } catch (SQLException e) {
            System.err.println("[ERROR] Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
