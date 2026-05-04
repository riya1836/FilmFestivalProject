package venue;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateVenueDialog extends JDialog {
    JTextField idField, nameField, addressField, capacityField;
    JButton updateButton;

    public UpdateVenueDialog(JFrame parent) {
        super(parent, "Update Venue", true);
        setSize(350, 250);
        setLayout(new GridLayout(5, 2, 5, 5));

        add(new JLabel("Venue ID (to update):"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("New Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("New Address:"));
        addressField = new JTextField();
        add(addressField);

        add(new JLabel("New Capacity:"));
        capacityField = new JTextField();
        add(capacityField);

        updateButton = new JButton("Update");
        add(updateButton);
        add(new JLabel());

        updateButton.addActionListener(e -> updateVenue());
        setVisible(true);
    }

    private void updateVenue() {
        Integer venueId = DialogUtils.parseIntField(this, idField, "Venue ID");
        Integer capacity = DialogUtils.parseIntField(this, capacityField, "Capacity");
        if (venueId == null || capacity == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE Venue SET venue_name=?, address=?, capacity=? WHERE venue_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nameField.getText());
            ps.setString(2, addressField.getText());
            ps.setInt(3, capacity);
            ps.setInt(4, venueId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Venue Updated Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No venue found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating venue: " + ex.getMessage());
        }
    }
}
