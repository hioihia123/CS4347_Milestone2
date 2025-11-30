package com.checkmates.ui.components;

import com.checkmates.model.Borrower;
import com.checkmates.util.BorrowerUtil;

import javax.swing.*;
import java.awt.*;

public class AddBorrowerPanel extends JPanel {

    private JTextField ssnField;
    private JTextField nameField;
    private JTextField addressField;
    private JTextField phoneField;
    private JButton submitBtn;

    public AddBorrowerPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Add New Borrower");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(60, 60, 60));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);

        // SSN
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        add(new JLabel("SSN:"), gbc);
        
        ssnField = new JTextField(20);
        ssnField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        add(ssnField, gbc);

        // Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        add(new JLabel("Name:"), gbc);
        
        nameField = new JTextField(20);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        add(nameField, gbc);

        // Address
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        add(new JLabel("Address:"), gbc);
        
        addressField = new JTextField(20);
        addressField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        add(addressField, gbc);

        // Phone
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        add(new JLabel("Phone:"), gbc);
        
        phoneField = new JTextField(20);
        phoneField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        add(phoneField, gbc);

        // Submit button
        submitBtn = new JButton("Add Borrower");
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        submitBtn.setBackground(new Color(70, 130, 180));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setPreferredSize(new Dimension(150, 40));
        submitBtn.addActionListener(e -> handleAddBorrower());
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        add(submitBtn, gbc);
        
        // Add some padding at bottom
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    private void handleAddBorrower() {
        String ssn = ssnField.getText().trim();
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();

        // Required validations
        if (ssn.isEmpty() || name.isEmpty() || address.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "SSN, Name, and Address are required fields.",
                    "Missing Fields",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create borrower object
        Borrower borrower = new Borrower(ssn, name, address, phone.isEmpty() ? null : phone);

        // Use the util to insert into DB
        BorrowerUtil util = new BorrowerUtil();
        boolean success = util.addBorrower(borrower);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Borrower added successfully!\nCard ID: " + borrower.getCardId(),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Clear the text fields
            ssnField.setText("");
            nameField.setText("");
            addressField.setText("");
            phoneField.setText("");

        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to add borrower.\nSSN may already exist in the system.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

