package screening;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddScreeningDialog extends JDialog {
    JTextField screening_idField, film_idField, venue_idField, screening_timeField;
    JButton addButton;

    public AddScreeningDialog(JFrame parent) {
        super(parent, "Add Screening", true);
        setSize(420, 250);
        setLayout(new GridLayout(5, 2, 5, 5));

        add(new JLabel("Screening ID:"));
        screening_idField = new JTextField();
        add(screening_idField);

        add(new JLabel("Film ID:"));
        film_idField = new JTextField();
        add(film_idField);

        add(new JLabel("Venue ID:"));
        venue_idField = new JTextField();
        add(venue_idField);

        add(new JLabel("Screening Time (YYYY-MM-DD HH:MM:SS):"));
        screening_timeField = new JTextField();
        add(screening_timeField);

        addButton = new JButton("Add");
        add(new JLabel());
        add(addButton);

        addButton.addActionListener(e -> addScreening());
        setVisible(true);
    }

    private void addScreening() {
        Integer screeningId = DialogUtils.parseIntField(this, screening_idField, "Screening ID");
        Integer filmId = DialogUtils.parseIntField(this, film_idField, "Film ID");
        Integer venueId = DialogUtils.parseIntField(this, venue_idField, "Venue ID");
        if (screeningId == null || filmId == null || venueId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO Screening (screening_id, film_id, venue_id, screening_time) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, screeningId);
            ps.setInt(2, filmId);
            ps.setInt(3, venueId);
            ps.setString(4, screening_timeField.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Screening Added Successfully");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding screening: " + ex.getMessage());
        }
    }
}
