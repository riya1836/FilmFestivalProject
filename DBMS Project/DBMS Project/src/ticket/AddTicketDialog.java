package ticket;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddTicketDialog extends JDialog {
    JTextField ticket_idField, screening_idField, attendee_idField, seat_numberField, priceField;
    JButton addButton;

    public AddTicketDialog(JFrame parent) {
        super(parent, "Add Ticket", true);
        setSize(420, 280);
        setLayout(new GridLayout(6, 2, 5, 5));

        add(new JLabel("Ticket ID:"));
        ticket_idField = new JTextField();
        add(ticket_idField);

        add(new JLabel("Screening ID:"));
        screening_idField = new JTextField();
        add(screening_idField);

        add(new JLabel("Attendee ID:"));
        attendee_idField = new JTextField();
        add(attendee_idField);

        add(new JLabel("Seat Number:"));
        seat_numberField = new JTextField();
        add(seat_numberField);

        add(new JLabel("Price:"));
        priceField = new JTextField();
        add(priceField);

        addButton = new JButton("Add");
        add(new JLabel());
        add(addButton);

        addButton.addActionListener(e -> addTicket());
        setVisible(true);
    }

    private void addTicket() {
        Integer ticketId = DialogUtils.parseIntField(this, ticket_idField, "Ticket ID");
        Integer screeningId = DialogUtils.parseIntField(this, screening_idField, "Screening ID");
        Integer attendeeId = DialogUtils.parseIntField(this, attendee_idField, "Attendee ID");
        Double price = DialogUtils.parseDoubleField(this, priceField, "Price");
        if (ticketId == null || screeningId == null || attendeeId == null || price == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO Ticket (ticket_id, screening_id, attendee_id, seat_number, price) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, ticketId);
            ps.setInt(2, screeningId);
            ps.setInt(3, attendeeId);
            ps.setString(4, seat_numberField.getText());
            ps.setDouble(5, price);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Ticket Added Successfully");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding ticket: " + ex.getMessage());
        }
    }
}
