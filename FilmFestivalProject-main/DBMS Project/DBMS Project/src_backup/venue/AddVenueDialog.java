package venue;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddVenueDialog extends JDialog {
    JTextField idField, nameField, addressField, capacityField;
    JButton saveButton;

    public AddVenueDialog(JFrame parent) {
        super(parent, "Add Venue", true);
        setSize(350, 250);
        setLayout(new GridLayout(5, 2, 5, 5));

        add(new JLabel("Venue ID:"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("Venue Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Address:"));
        addressField = new JTextField();
        add(addressField);

        add(new JLabel("Capacity:"));
        capacityField = new JTextField();
        add(capacityField);

        saveButton = new JButton("Save");
        add(saveButton);
        add(new JLabel());

        saveButton.addActionListener(e -> saveVenue());
        setVisible(true);
    }

    private void saveVenue() {
        Integer venueId = DialogUtils.parseIntField(this, idField, "Venue ID");
        Integer capacity = DialogUtils.parseIntField(this, capacityField, "Capacity");
        if (venueId == null || capacity == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO Venue (venue_id, venue_name, address, capacity) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, venueId);
            ps.setString(2, nameField.getText());
            ps.setString(3, addressField.getText());
            ps.setInt(4, capacity);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Venue Added Successfully");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding venue: " + ex.getMessage());
        }
    }
}
