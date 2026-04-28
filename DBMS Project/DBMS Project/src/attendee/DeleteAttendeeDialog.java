package attendee;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteAttendeeDialog extends JDialog {
    JTextField idField;
    JButton deleteBtn;

    public DeleteAttendeeDialog() {
        setTitle("Delete Attendee");
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Attendee ID to Delete:"));
        idField = new JTextField();
        add(idField);

        deleteBtn = new JButton("Delete");
        add(new JLabel());
        add(deleteBtn);

        deleteBtn.addActionListener(e -> deleteAttendee());
        setVisible(true);
    }

    private void deleteAttendee() {
        Integer attendeeId = DialogUtils.parseIntField(this, idField, "Attendee ID");
        if (attendeeId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM attendee WHERE attendee_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, attendeeId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Attendee deleted successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No attendee found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting attendee: " + ex.getMessage());
        }
    }
}
