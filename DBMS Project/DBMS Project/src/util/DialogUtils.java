package util;

import javax.swing.*;
import java.awt.*;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static Integer parseIntField(Component parent, JTextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            showValidationError(parent, field, fieldName + " is required.");
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            showValidationError(parent, field, fieldName + " must be a whole number.");
            return null;
        }
    }

    public static Double parseDoubleField(Component parent, JTextField field, String fieldName) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            showValidationError(parent, field, fieldName + " is required.");
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            showValidationError(parent, field, fieldName + " must be a valid number.");
            return null;
        }
    }

    private static void showValidationError(Component parent, JTextField field, String message) {
        JOptionPane.showMessageDialog(parent, message, "Invalid Input", JOptionPane.WARNING_MESSAGE);
        field.requestFocusInWindow();
        field.selectAll();
    }
}
