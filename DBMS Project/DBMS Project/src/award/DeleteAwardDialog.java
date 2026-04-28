package award;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteAwardDialog extends JDialog {
    JTextField award_idField;
    JButton deleteButton;

    public DeleteAwardDialog(JFrame parent) {
        super(parent, "Delete Award", true);
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));

        add(new JLabel("Award ID:"));
        award_idField = new JTextField();
        add(award_idField);

        deleteButton = new JButton("Delete");
        add(deleteButton);
        add(new JLabel());

        deleteButton.addActionListener(e -> deleteAward());
        setVisible(true);
    }

    private void deleteAward() {
        Integer awardId = DialogUtils.parseIntField(this, award_idField, "Award ID");
        if (awardId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM award WHERE award_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, awardId);
            int rowsDeleted = ps.executeUpdate();

            if (rowsDeleted > 0) {
                JOptionPane.showMessageDialog(this, "Award Deleted Successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "No award found with that ID!");
            }

            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting award: " + ex.getMessage());
        }
    }
}
