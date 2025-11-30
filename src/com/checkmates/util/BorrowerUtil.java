package com.checkmates.util;

import com.checkmates.model.Borrower;
import java.sql.*;
import javax.swing.JOptionPane;

public class BorrowerUtil {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    /** Check if SSN already exists */
    public boolean ssnExists(String ssn) {
        String sql = "SELECT ssn FROM borrower WHERE ssn = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            System.err.println("Database error checking SSN: " + e.getMessage());
            e.printStackTrace();
            // Return false on error so we can see the actual error during insert
            // This prevents masking connection issues as "duplicate SSN"
            return false;
        }
    }

    /** Generate new card_id */
    public String generateNewCardId() {
        String sql = "SELECT MAX(card_id) AS max_id FROM borrower";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                return String.valueOf(maxId + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "10001";
    }

    /** Add a new borrower */
    public boolean addBorrower(Borrower b) {

        if (b.getName() == null || b.getName().isEmpty()) return false;
        if (b.getSsn() == null || b.getSsn().isEmpty()) return false;
        if (b.getAddress() == null || b.getAddress().isEmpty()) return false;

        if (ssnExists(b.getSsn())) {
            System.out.println("Error: Borrower with this SSN already exists.");
            return false;
        }

        String newCardId = generateNewCardId();
        b.setCardId(newCardId);

        String sql = "INSERT INTO borrower (card_id, ssn, bname, address, phone) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, b.getCardId());
            stmt.setString(2, b.getSsn());
            stmt.setString(3, b.getName());
            stmt.setString(4, b.getAddress());
            stmt.setString(5, b.getPhone());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Database error adding borrower: " + e.getMessage());
            e.printStackTrace();
            // Show more detailed error to user
            JOptionPane.showMessageDialog(null, 
                "Database Error: " + e.getMessage() + "\n\nCheck:\n- Database is running\n- Correct password in DatabaseConnection.java\n- Table 'borrower' exists",
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
