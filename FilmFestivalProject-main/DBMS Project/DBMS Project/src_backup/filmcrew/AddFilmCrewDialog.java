package filmcrew;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddFilmCrewDialog extends JDialog {
    JTextField crew_idField, film_idField, nameField, phone_noField, roleField;
    JButton addButton;

    public AddFilmCrewDialog(JFrame parent) {
        super(parent, "Add Film Crew", true);
        setSize(400, 300);
        setLayout(new GridLayout(6, 2, 5, 5));

        add(new JLabel("Crew ID:"));
        crew_idField = new JTextField();
        add(crew_idField);

        add(new JLabel("Film ID:"));
        film_idField = new JTextField();
        add(film_idField);

        add(new JLabel("Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Phone Number:"));
        phone_noField = new JTextField();
        add(phone_noField);

        add(new JLabel("Role:"));
        roleField = new JTextField();
        add(roleField);

        addButton = new JButton("Add");
        add(new JLabel());
        add(addButton);

        addButton.addActionListener(e -> addFilmCrew());
        setVisible(true);
    }

    private void addFilmCrew() {
        Integer crewId = DialogUtils.parseIntField(this, crew_idField, "Crew ID");
        Integer filmId = DialogUtils.parseIntField(this, film_idField, "Film ID");
        if (crewId == null || filmId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO Film_Crew (crew_id, film_id, name, phone_no, role) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, crewId);
            ps.setInt(2, filmId);
            ps.setString(3, nameField.getText());
            ps.setString(4, phone_noField.getText());
            ps.setString(5, roleField.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Film Crew Added Successfully!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding film crew: " + ex.getMessage());
        }
    }
}

