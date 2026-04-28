package screening;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateScreeningDialog extends JDialog {
    JTextField screening_idField, screening_timeField;
    JButton updateButton;

    public UpdateScreeningDialog(JFrame parent) {
        super(parent, "Update Screening Time", true);
        setSize(420, 180);
        setLayout(new GridLayout(3, 2, 5, 5));

        add(new JLabel("Screening ID:"));
        screening_idField = new JTextField();
        add(screening_idField);

        add(new JLabel("New Screening Time (YYYY-MM-DD HH:MM:SS):"));
        screening_timeField = new JTextField();
        add(screening_timeField);

        updateButton = new JButton("Update");
        add(new JLabel());
        add(updateButton);

        updateButton.addActionListener(e -> updateScreening());
        setVisible(true);
    }

    private void updateScreening() {
        Integer screeningId = DialogUtils.parseIntField(this, screening_idField, "Screening ID");
        if (screeningId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE Screening SET screening_time = ? WHERE screening_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, screening_timeField.getText());
            ps.setInt(2, screeningId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Screening Updated Successfully");
            } else {
                JOptionPane.showMessageDialog(this, "Screening ID not found");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating screening: " + ex.getMessage());
        }
    }
}
