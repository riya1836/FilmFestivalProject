package venue;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteVenueDialog extends JDialog {
    JTextField idField;
    JButton deleteButton;

    public DeleteVenueDialog(JFrame parent) {
        super(parent, "Delete Venue", true);
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));

        add(new JLabel("Venue ID to Delete:"));
        idField = new JTextField();
        add(idField);

        deleteButton = new JButton("Delete");
        add(new JLabel());
        add(deleteButton);

        deleteButton.addActionListener(e -> deleteVenue());
        setVisible(true);
    }

    private void deleteVenue() {
        Integer venueId = DialogUtils.parseIntField(this, idField, "Venue ID");
        if (venueId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM Venue WHERE venue_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, venueId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Venue Deleted Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No venue found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting venue: " + ex.getMessage());
        }
    }
}
