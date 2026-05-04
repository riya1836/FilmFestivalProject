package attendee;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddAttendeeDialog extends JDialog {
    JTextField idField, firstNameField, lastNameField, emailField, phoneField;
    JButton addButton;

    public AddAttendeeDialog() {
        setTitle("Add Attendee");
        setSize(400, 300);
        setLayout(new GridLayout(6, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Attendee ID:"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("First Name:"));
        firstNameField = new JTextField();
        add(firstNameField);

        add(new JLabel("Last Name:"));
        lastNameField = new JTextField();
        add(lastNameField);

        add(new JLabel("Email:"));
        emailField = new JTextField();
        add(emailField);

        add(new JLabel("Phone:"));
        phoneField = new JTextField();
        add(phoneField);

        addButton = new JButton("Add");
        add(new JLabel());
        add(addButton);

        addButton.addActionListener(e -> addAttendee());
        setVisible(true);
    }

    private void addAttendee() {
        Integer attendeeId = DialogUtils.parseIntField(this, idField, "Attendee ID");
        if (attendeeId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO attendee (attendee_id, first_name, last_name, email, phone) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, attendeeId);
            ps.setString(2, firstNameField.getText());
            ps.setString(3, lastNameField.getText());
            ps.setString(4, emailField.getText());
            ps.setString(5, phoneField.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Attendee added successfully");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding attendee: " + ex.getMessage());
        }
    }
}
