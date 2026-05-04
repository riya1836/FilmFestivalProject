package app;

import db.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestInsertAward {
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            System.out.println("[TEST] Inserting award with film_id=0, crew_id=1");
            st.executeUpdate("INSERT INTO award (award_id, award_name, film_id, crew_id, year) VALUES (99991, 'TestCrew', 0, 1, 2026)");
            System.out.println("[TEST] Insert succeeded");
        } catch (SQLException e) {
            System.err.println("[TEST ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
