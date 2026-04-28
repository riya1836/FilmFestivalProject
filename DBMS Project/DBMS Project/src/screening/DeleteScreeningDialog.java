package screening;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteScreeningDialog extends JDialog {
    JTextField screening_idField;
    JButton deleteButton;

    public DeleteScreeningDialog(JFrame parent) {
        super(parent, "Delete Screening", true);
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));

        add(new JLabel("Screening ID:"));
        screening_idField = new JTextField();
        add(screening_idField);

        deleteButton = new JButton("Delete");
        add(new JLabel());
        add(deleteButton);

        deleteButton.addActionListener(e -> deleteScreening());
        setVisible(true);
    }

    private void deleteScreening() {
        Integer screeningId = DialogUtils.parseIntField(this, screening_idField, "Screening ID");
        if (screeningId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM Screening WHERE screening_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, screeningId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Screening Deleted Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Screening ID not found");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting screening: " + ex.getMessage());
        }
    }
}
