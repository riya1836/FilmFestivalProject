package attendee;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateAttendeeDialog extends JDialog {
    JTextField idField, firstNameField, lastNameField, emailField, phoneField;
    JButton updateBtn;

    public UpdateAttendeeDialog() {
        setTitle("Update Attendee");
        setSize(400, 300);
        setLayout(new GridLayout(6, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Attendee ID (to update):"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("New First Name:"));
        firstNameField = new JTextField();
        add(firstNameField);

        add(new JLabel("New Last Name:"));
        lastNameField = new JTextField();
        add(lastNameField);

        add(new JLabel("New Email:"));
        emailField = new JTextField();
        add(emailField);

        add(new JLabel("New Phone:"));
        phoneField = new JTextField();
        add(phoneField);

        updateBtn = new JButton("Update");
        add(new JLabel());
        add(updateBtn);

        updateBtn.addActionListener(e -> updateAttendee());
        setVisible(true);
    }

    private void updateAttendee() {
        Integer attendeeId = DialogUtils.parseIntField(this, idField, "Attendee ID");
        if (attendeeId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE attendee SET first_name=?, last_name=?, email=?, phone=? WHERE attendee_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, firstNameField.getText());
            ps.setString(2, lastNameField.getText());
            ps.setString(3, emailField.getText());
            ps.setString(4, phoneField.getText());
            ps.setInt(5, attendeeId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Attendee updated successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No attendee found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating attendee: " + ex.getMessage());
        }
    }
}
