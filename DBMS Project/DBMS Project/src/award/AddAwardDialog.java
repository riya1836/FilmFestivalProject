package award;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddAwardDialog extends JDialog {
    JTextField award_idField, award_nameField, film_idField, yearField;
    JButton saveButton;

    public AddAwardDialog(JFrame parent) {
        super(parent, "Add Award", true);
        setSize(350, 250);
        setLayout(new GridLayout(5, 2, 5, 5));

        add(new JLabel("Award ID:"));
        award_idField = new JTextField();
        add(award_idField);

        add(new JLabel("Award Name:"));
        award_nameField = new JTextField();
        add(award_nameField);

        add(new JLabel("Film ID:"));
        film_idField = new JTextField();
        add(film_idField);

        add(new JLabel("Year:"));
        yearField = new JTextField();
        add(yearField);

        saveButton = new JButton("Save");
        add(saveButton);
        add(new JLabel());

        saveButton.addActionListener(e -> addAward());
        setVisible(true);
    }

    private void addAward() {
        Integer awardId = DialogUtils.parseIntField(this, award_idField, "Award ID");
        Integer filmId = DialogUtils.parseIntField(this, film_idField, "Film ID");
        Integer year = DialogUtils.parseIntField(this, yearField, "Year");
        if (awardId == null || filmId == null || year == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO award (award_id, award_name, film_id, year) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, awardId);
            ps.setString(2, award_nameField.getText());
            ps.setInt(3, filmId);
            ps.setInt(4, year);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Award Added Successfully!");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding award: " + ex.getMessage());
        }
    }
}
