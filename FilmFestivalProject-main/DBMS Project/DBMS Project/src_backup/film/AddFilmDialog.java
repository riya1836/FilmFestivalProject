package film;

import db.DBConnection;
import util.DialogUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddFilmDialog extends JDialog {
    JTextField idField, titleField, runtimeField, langField, genreField;
    JButton addButton;

    public AddFilmDialog() {
        setTitle("Add Film");
        setSize(400, 300);
        setLayout(new GridLayout(6, 2, 5, 5));
        setLocationRelativeTo(null);

        add(new JLabel("Film ID:"));
        idField = new JTextField();
        add(idField);

        add(new JLabel("Title:"));
        titleField = new JTextField();
        add(titleField);

        add(new JLabel("Runtime:"));
        runtimeField = new JTextField();
        add(runtimeField);

        add(new JLabel("Language:"));
        langField = new JTextField();
        add(langField);

        add(new JLabel("Genre:"));
        genreField = new JTextField();
        add(genreField);

        addButton = new JButton("Add");
        add(new JLabel());
        add(addButton);

        addButton.addActionListener(e -> addFilm());
        setVisible(true);
    }

    private void addFilm() {
        Integer filmId = DialogUtils.parseIntField(this, idField, "Film ID");
        Integer runtime = DialogUtils.parseIntField(this, runtimeField, "Runtime");
        if (filmId == null || runtime == null) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO film (film_id, title, runtime, language, genre) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, filmId);
            ps.setString(2, titleField.getText());
            ps.setInt(3, runtime);
            ps.setString(4, langField.getText());
            ps.setString(5, genreField.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Film added successfully");
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error adding film: " + ex.getMessage());
        }
    }
}
