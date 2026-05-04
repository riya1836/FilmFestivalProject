package filmcrew;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class DeleteFilmCrewDialog extends JDialog {
    JTextField crew_idField;
    JButton deleteButton;

    public DeleteFilmCrewDialog(JFrame parent) {
        super(parent, "Delete Film Crew", true);
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));

        add(new JLabel("Crew ID:"));
        crew_idField = new JTextField();
        add(crew_idField);

        deleteButton = new JButton("Delete");
        add(new JLabel());
        add(deleteButton);

        deleteButton.addActionListener(e -> deleteFilmCrew());
        setVisible(true);
    }

    private void deleteFilmCrew() {
        Integer crewId = DialogUtils.parseIntField(this, crew_idField, "Crew ID");
        if (crewId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM Film_Crew WHERE crew_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, crewId);
            int rows = ps.executeUpdate();

            if (rows > 0)
                JOptionPane.showMessageDialog(this, "Film Crew Deleted Successfully!");
            else
                JOptionPane.showMessageDialog(this, "Crew ID not found!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting film crew: " + ex.getMessage());
        }
    }
}

