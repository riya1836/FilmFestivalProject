package ticket;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteTicketDialog extends JDialog {
    JTextField ticket_idField;
    JButton deleteButton;

    public DeleteTicketDialog(JFrame parent) {
        super(parent, "Delete Ticket", true);
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));

        add(new JLabel("Ticket ID:"));
        ticket_idField = new JTextField();
        add(ticket_idField);

        deleteButton = new JButton("Delete");
        add(new JLabel());
        add(deleteButton);

        deleteButton.addActionListener(e -> deleteTicket());
        setVisible(true);
    }

    private void deleteTicket() {
        Integer ticketId = DialogUtils.parseIntField(this, ticket_idField, "Ticket ID");
        if (ticketId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM Ticket WHERE ticket_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, ticketId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Ticket Deleted Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Ticket ID not found");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting ticket: " + ex.getMessage());
        }
    }
}
