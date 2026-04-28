package app;

import db.DBConnection;
import venue.AddVenueDialog;
import venue.DeleteVenueDialog;
import venue.UpdateVenueDialog;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VenueFrame extends JFrame {
    JButton addButton, viewButton, updateButton, deleteButton;

    public VenueFrame() {
        setTitle("Film Management");
        setSize(350, 250);
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        addButton = new JButton("Add Venue");
        updateButton = new JButton("Update Venue");
        deleteButton = new JButton("Delete Venue");
        viewButton = new JButton("View Venues");

        // Add buttons in vertical order
        add(addButton, gbc);

        gbc.gridy++;
        add(viewButton, gbc);

        gbc.gridy++;
        add(updateButton, gbc);

        gbc.gridy++;
        add(deleteButton, gbc);


        addButton.addActionListener(e -> new AddVenueDialog(this));
        updateButton.addActionListener(e -> new UpdateVenueDialog(this));
        deleteButton.addActionListener(e -> new DeleteVenueDialog(this));
        viewButton.addActionListener(e -> viewVenues());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void viewVenues() {
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Venue")) {

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
            JOptionPane.showMessageDialog(this, new JScrollPane(table), "All Venues", JOptionPane.PLAIN_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error displaying venues: " + ex.getMessage());
        }
    }
}
