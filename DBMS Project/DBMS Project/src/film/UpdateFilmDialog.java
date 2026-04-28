package film;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateFilmDialog extends JDialog {
    JTextField idField, titleField, runtimeField, langField, genreField;
    JButton updateBtn;

    public UpdateFilmDialog() {
        setTitle("Update Film");
        setSize(400, 300);
        setLayout(new GridLayout(6, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Film ID (to update):"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("New Title:"));
        titleField = new JTextField();
        add(titleField);

        add(new JLabel("New Runtime:"));
        runtimeField = new JTextField();
        add(runtimeField);

        add(new JLabel("New Language:"));
        langField = new JTextField();
        add(langField);

        add(new JLabel("New Genre:"));
        genreField = new JTextField();
        add(genreField);

        updateBtn = new JButton("Update");
        add(new JLabel());
        add(updateBtn);

        updateBtn.addActionListener(e -> updateFilm());
        setVisible(true);
    }

    private void updateFilm() {
        Integer filmId = DialogUtils.parseIntField(this, idField, "Film ID");
        Integer runtime = DialogUtils.parseIntField(this, runtimeField, "Runtime");
        if (filmId == null || runtime == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "UPDATE film SET title=?, runtime=?, language=?, genre=? WHERE film_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, titleField.getText());
            ps.setInt(2, runtime);
            ps.setString(3, langField.getText());
            ps.setString(4, genreField.getText());
            ps.setInt(5, filmId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Film updated successfully");
            } else {
                JOptionPane.showMessageDialog(this, "No film found with that ID");
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating film: " + ex.getMessage());
        }
    }
}
