package film;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteFilmDialog extends JDialog {
    JTextField idField;
    JButton deleteBtn;

    public DeleteFilmDialog() {
        setTitle("Delete Film");
        setSize(300, 150);
        setLayout(new GridLayout(2, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Film ID to Delete:"));
        idField = new JTextField();
        add(idField);

        deleteBtn = new JButton("Delete");
        add(new JLabel());
        add(deleteBtn);

        deleteBtn.addActionListener(e -> deleteFilm());
        setVisible(true);
    }

    private void deleteFilm() {
        Integer filmId = DialogUtils.parseIntField(this, idField, "Film ID");
        if (filmId == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM film WHERE film_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Film deleted successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No film found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error deleting film: " + ex.getMessage());
        }
    }
}
