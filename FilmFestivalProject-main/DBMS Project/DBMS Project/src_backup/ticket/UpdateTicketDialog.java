package ticket;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateTicketDialog extends JDialog {
    JTextField ticket_idField, priceField;
    JButton updateButton;

    public UpdateTicketDialog(JFrame parent) {
        super(parent, "Update Ticket Price", true);
        setSize(300, 150);
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Ticket ID:"));
        ticket_idField = new JTextField();
        add(ticket_idField);

        add(new JLabel("New Price:"));
        priceField = new JTextField();
        add(priceField);

        updateButton = new JButton("Update");
        add(new JLabel());
        add(updateButton);

        updateButton.addActionListener(e -> updateTicket());
        setVisible(true);
    }

    private void updateTicket() {
        Integer ticketId = DialogUtils.parseIntField(this, ticket_idField, "Ticket ID");
        Double price = DialogUtils.parseDoubleField(this, priceField, "Price");
        if (ticketId == null || price == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE Ticket SET price = ? WHERE ticket_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDouble(1, price);
            ps.setInt(2, ticketId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Ticket Updated Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Ticket ID not found");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating ticket: " + ex.getMessage());
        }
    }
}
