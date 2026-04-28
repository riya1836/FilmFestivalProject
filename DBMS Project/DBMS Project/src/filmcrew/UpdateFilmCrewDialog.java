package filmcrew;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class UpdateFilmCrewDialog extends JDialog {
    JTextField crew_idField, roleField;
    JButton updateButton;

    public UpdateFilmCrewDialog(JFrame parent) {
        super(parent, "Update Film Crew Role", true);
        setSize(300, 200);
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Crew ID:"));
        crew_idField = new JTextField();
        add(crew_idField);

        add(new JLabel("New Role:"));
        roleField = new JTextField();
        add(roleField);

        updateButton = new JButton("Update");
        add(new JLabel());
        add(updateButton);

        updateButton.addActionListener(e -> updateFilmCrew());
        setVisible(true);
    }

    private void updateFilmCrew() {
        Integer crewId = DialogUtils.parseIntField(this, crew_idField, "Crew ID");
        if (crewId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE Film_Crew SET role = ? WHERE crew_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, roleField.getText());
            ps.setInt(2, crewId);
            int rows = ps.executeUpdate();

            if (rows > 0)
                JOptionPane.showMessageDialog(this, "Role Updated Successfully!");
            else
                JOptionPane.showMessageDialog(this, "Crew ID not found!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating film crew: " + ex.getMessage());
        }
    }
}

