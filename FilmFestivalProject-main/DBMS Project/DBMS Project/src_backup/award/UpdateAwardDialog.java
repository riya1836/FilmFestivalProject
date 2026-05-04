package award;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateAwardDialog extends JDialog {
    JTextField award_idField, award_nameField, film_idField, yearField;
    JButton updateButton;

    public UpdateAwardDialog(JFrame parent) {
        super(parent, "Update Award", true);
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

        updateButton = new JButton("Update");
        add(updateButton);
        add(new JLabel());

        updateButton.addActionListener(e -> updateAward());
        setVisible(true);
    }

    private void updateAward() {
        Integer awardId = DialogUtils.parseIntField(this, award_idField, "Award ID");
        Integer filmId = DialogUtils.parseIntField(this, film_idField, "Film ID");
        Integer year = DialogUtils.parseIntField(this, yearField, "Year");
        if (awardId == null || filmId == null || year == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE award SET award_name=?, film_id=?, year=? WHERE award_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, award_nameField.getText());
            ps.setInt(2, filmId);
            ps.setInt(3, year);
            ps.setInt(4, awardId);
            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(this, "Award Updated Successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "No award found with that ID!");
            }

            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating award: " + ex.getMessage());
        }
    }
}
