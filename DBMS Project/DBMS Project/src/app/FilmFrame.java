package app;

import db.DBConnection;
import film.AddFilmDialog;
import film.DeleteFilmDialog;
import film.UpdateFilmDialog;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmFrame extends JFrame {
    public FilmFrame() {
        setTitle("Film Management");
        setSize(350, 250);
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JButton addButton = new JButton("Add Film");
        JButton viewButton = new JButton("View Films");
        JButton updateButton = new JButton("Update Film");
        JButton deleteButton = new JButton("Delete Film");

        // Add buttons in vertical order
        add(addButton, gbc);

        gbc.gridy++;
        add(viewButton, gbc);

        gbc.gridy++;
        add(updateButton, gbc);

        gbc.gridy++;
        add(deleteButton, gbc);

        // Action listeners to open new dialogs
        addButton.addActionListener(e -> new AddFilmDialog());
        viewButton.addActionListener(e -> viewFilms());
        updateButton.addActionListener(e -> new UpdateFilmDialog());
        deleteButton.addActionListener(e -> new DeleteFilmDialog());

        setVisible(true);
    }

    // View Films (Display all in a JTable)
    private void viewFilms() {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM film";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            String[] columnNames = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = meta.getColumnName(i);
            }

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] rowData = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    rowData[i - 1] = rs.getString(i);
                }
                rows.add(rowData);
            }

            String[][] data = rows.toArray(new String[0][]);
            JTable table = new JTable(data, columnNames);
            JOptionPane.showMessageDialog(this, new JScrollPane(table), "All Films", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error displaying films: " + ex.getMessage());
        }
    }
}
