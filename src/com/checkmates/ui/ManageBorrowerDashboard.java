package com.checkmates.ui;

import com.checkmates.model.Librarian;
import com.checkmates.ui.components.AddBorrowerPanel;
import com.checkmates.ui.components.FancyHoverButton;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Borrower Management Dashboard
 * Allows librarians to manage borrowers (add new borrowers)
 */
public class ManageBorrowerDashboard extends JFrame {

    private Librarian lib;
    
    // Modern style properties
    private Color modernTextColor = new Color(60, 60, 60);
    private Font modernFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font modernTitleFont = new Font("Segoe UI", Font.BOLD, 24);
    private Color modernBackground = Color.WHITE;
    private Color modernPanelColor = new Color(240, 240, 240);

    public ManageBorrowerDashboard(Librarian lib) {
        this.lib = lib;
        setTitle("Borrower Management");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupLookAndFeel();
        initComponents();
    }

    private void setupLookAndFeel() {
        try {
            UIManager.put("OptionPane.background", modernBackground);
            UIManager.put("OptionPane.messageFont", modernFont);
            UIManager.put("OptionPane.messageForeground", modernTextColor);
            UIManager.put("TextField.background", modernBackground);
            UIManager.put("TextField.font", modernFont);
            UIManager.put("TextField.foreground", modernTextColor);
            UIManager.put("TextField.border", BorderFactory.createLineBorder(new Color(200, 200, 200)));
            UIManager.put("Label.font", modernFont);
            UIManager.put("Label.foreground", modernTextColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Top Panel with Title ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Borrower Management");
        titleLabel.setFont(modernTitleFont);
        titleLabel.setForeground(modernTextColor);
        topPanel.add(titleLabel);

        // --- Center Panel with Add Borrower Form ---
        AddBorrowerPanel addBorrowerPanel = new AddBorrowerPanel();
        addBorrowerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Create New Borrower",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 16)
            ),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        centerPanel.add(addBorrowerPanel, gbc);

        // --- Bottom Panel with Close Button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(Color.WHITE);

        FancyHoverButton closeButton = new FancyHoverButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);

        // --- Add everything to main panel ---
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
    }

    // Main for testing
    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(new FlatLightLaf()); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        Librarian dummy = new Librarian("Test Lib", "test@test.com", "L001");
        SwingUtilities.invokeLater(() -> new ManageBorrowerDashboard(dummy).setVisible(true));
    }
}

