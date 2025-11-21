package com.checkmates.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import javax.swing.RowFilter;

import com.checkmates.model.Librarian;
import com.checkmates.ui.components.FancyHoverButton;
import com.checkmates.ui.components.FancyHoverButton2;

import com.formdev.flatlaf.FlatLightLaf;

public class ManageBooksDashboard extends JFrame {

    private Librarian lib;
    private JTable booksTable;
    private JTextField searchField;
    private TableRowSorter<TableModel> rowSorter;

    // Modern style properties
    private Color modernTextColor = new Color(60, 60, 60);
    private Font modernFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font modernTitleFont = new Font("Segoe UI", Font.BOLD, 24);
    private Color modernBackground = Color.WHITE;
    private Color modernPanelColor = new Color(240, 240, 240);
    private Color modernHighlightColor = new Color(200, 220, 255);

    public ManageBooksDashboard(Librarian lib) {
        this.lib = lib;
        setTitle("Library Catalog & Management");
        setSize(1200, 800);
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Book Catalog");
        titleLabel.setFont(modernTitleFont);
        titleLabel.setForeground(modernTextColor);
        topPanel.add(titleLabel);

        // --- Table Setup ---
        booksTable = new JTable();
        customizeTable();

        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove border
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        // Use your existing ModernScrollBarUI from Note.java/Dashboard.java
        // If you don't have the class file, define it as an inner class (see below)
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        // --- Bottom Panel (Search & Buttons) ---
        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBackground(Color.WHITE);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel("Search (ISBN, Title, Author): "));
        searchField = new JTextField(25);
        searchPanel.add(searchField);
        
        // Search Listener (Real-time search via PHP)
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }
        });

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Color.WHITE);

        FancyHoverButton addButton = new FancyHoverButton("Add Book");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        addButton.addActionListener(e -> addNewBook());
        buttonPanel.add(addButton);

        FancyHoverButton editButton = new FancyHoverButton("Edit Selected");
        editButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        editButton.addActionListener(e -> editSelectedBook());
        buttonPanel.add(editButton);

        FancyHoverButton deleteButton = new FancyHoverButton("Delete Selected");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        deleteButton.addActionListener(e -> deleteSelectedBook());
        buttonPanel.add(deleteButton);
        
        FancyHoverButton2 checkoutButton = new FancyHoverButton2("Checkout Book");
        checkoutButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        checkoutButton.addActionListener(e -> checkoutBook());
        buttonPanel.add(checkoutButton);
        
        FancyHoverButton2 checkinButton = new FancyHoverButton2("Checkin Book");
        checkinButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        checkinButton.addActionListener(e -> deleteSelectedBook());
        buttonPanel.add(checkinButton);

        FancyHoverButton refreshButton = new FancyHoverButton("\u27F3");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        refreshButton.addActionListener(e -> loadAllBooks());
        buttonPanel.add(refreshButton);
        
        bottomContainer.add(searchPanel, BorderLayout.NORTH);
        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);

        // Add everything to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomContainer, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        // Load data on start
        loadAllBooks();
    }
    
    // --- Networking & Logic ---

    private void loadAllBooks() {
        new Thread(() -> {
            try {
                // You need to create getBooks.php that returns all books with status
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/getBooks.php"; 
                String response = fetchUrl(urlString);
                updateTableWithJson(response);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
            }
        }).start();
    }

    private void updateTableWithJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            if ("success".equalsIgnoreCase(json.optString("status"))) {
                JSONArray booksArray = json.getJSONArray("books");
                
                // Columns required by Milestone 2
                String[] columnNames = {"ISBN", "Book Title", "Authors", "Availability"};
                ArrayList<String[]> rowData = new ArrayList<>();

                for (int i = 0; i < booksArray.length(); i++) {
                    JSONObject obj = booksArray.getJSONObject(i);
                    String isbn = obj.optString("Isbn");
                    String title = obj.optString("Title");
                    String authors = obj.optString("Authors"); // Comma separated
                    String availability = obj.optString("Availability"); // "IN" or "OUT"
                    
                    rowData.add(new String[]{isbn, title, authors, availability});
                }

                String[][] data = rowData.toArray(new String[0][]);
                
                SwingUtilities.invokeLater(() -> {
                    DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                        @Override
                        public boolean isCellEditable(int row, int column) { return false; }
                    };
                    booksTable.setModel(model);
                    customizeTableColumns();
                    
                    rowSorter = new TableRowSorter<>(model);
                    booksTable.setRowSorter(rowSorter);
                });

            } else {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error: " + json.optString("message")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- CRUD Operations ---

    private void addNewBook() {
        JDialog dialog = new JDialog(this, "Add New Book", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField isbnField = new JTextField(20);
        JTextField titleField = new JTextField(20);
        // For simplicity, asking for Author ID. Ideally this would be a dropdown or search.
        JTextField authorIdField = new JTextField(20); 

        addFormRow(panel, gbc, 0, "ISBN:", isbnField);
        addFormRow(panel, gbc, 1, "Title:", titleField);
        addFormRow(panel, gbc, 2, "Author ID:", authorIdField);

        JButton saveButton = createModernButton("Save Book");
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String isbn = isbnField.getText().trim();
            String title = titleField.getText().trim();
            String authId = authorIdField.getText().trim();

            if (isbn.isEmpty() || title.isEmpty() || authId.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.");
                return;
            }

            sendBookData(isbn, title, authId); // Create this PHP
            dialog.dispose();
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void editSelectedBook() {
        int row = booksTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to edit.");
            return;
        }
        
        String currentIsbn = (String) booksTable.getValueAt(row, 0);
        String currentTitle = (String) booksTable.getValueAt(row, 1);

        // Simplified Edit Dialog (only title for now as ISBN is PK)
        String newTitle = JOptionPane.showInputDialog(this, "Edit Title for " + currentIsbn, currentTitle);
        if (newTitle != null && !newTitle.trim().isEmpty()) {
             // You will need updateBook.php
             sendBookUpdate(currentIsbn, newTitle);
        }
    }

    private void deleteSelectedBook() {
        int row = booksTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.");
            return;
        }

        String isbn = (String) booksTable.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete book " + isbn + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    String urlString = "http://cm8tes.com/CS4347_Project_Folder/deleteBook.php";
                    String params = "isbn=" + URLEncoder.encode(isbn, "UTF-8");
                    // 1. Send Request and GET THE RESPONSE STRING
                    String response = postDataWithResponse(urlString, params);

                    // 2. Parse JSON
                    JSONObject json = new JSONObject(response);
                    String status = json.optString("status");
                    String message = json.optString("message");

                    // 3. Update UI on Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        if ("success".equalsIgnoreCase(status)) {
                            showModernDialog("Success", message, true);
                            loadAllBooks(); // Refresh table only on success
                        } else {
                            showModernDialog("Error", message, false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // --- Helper Methods ---

    private void sendBookData(String isbn, String title, String authorId) {
        new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/addBook.php";
                String params = "isbn=" + URLEncoder.encode(isbn, "UTF-8") +
                                "&title=" + URLEncoder.encode(title, "UTF-8") +
                                "&author_id=" + URLEncoder.encode(authorId, "UTF-8");

                // 1. Send Request and GET THE RESPONSE STRING
                String response = postDataWithResponse(urlString, params);
                
                // 2. Parse JSON
                JSONObject json = new JSONObject(response);
                String status = json.optString("status");
                String message = json.optString("message");

                // 3. Update UI on Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showModernDialog("Success", message, true);
                        loadAllBooks(); // Refresh table only on success
                    } else {
                        showModernDialog("Error", message, false);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    showModernDialog("Connection Error", e.getMessage(), false));
            }
        }).start();
    }
    
    private void sendBookUpdate(String isbn, String title) {
         new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/updateBook.php";
                String params = "isbn=" + URLEncoder.encode(isbn, "UTF-8") +
                                "&title=" + URLEncoder.encode(title, "UTF-8");
                
                // 1. Send Request and GET THE RESPONSE STRING
                String response = postDataWithResponse(urlString, params);
                
                // 2. Parse JSON
                JSONObject json = new JSONObject(response);
                String status = json.optString("status");
                String message = json.optString("message");

                // 3. Update UI on Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showModernDialog("Success", message, true);
                        loadAllBooks(); // Refresh table only on success
                    } else {
                        showModernDialog("Error", message, false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String fetchUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();
        return response.toString();
    }
    
    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private void customizeTable() {
        // 1. Fonts and Colors
        booksTable.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Cleaner font
        booksTable.setForeground(new Color(50, 50, 50)); // Dark grey text
        booksTable.setRowHeight(40); // Taller rows for a card-like feel
        booksTable.setShowVerticalLines(false); // Cleaner look
        booksTable.setShowHorizontalLines(true);
        booksTable.setGridColor(new Color(230, 230, 230)); // Very light grey lines
        booksTable.setIntercellSpacing(new Dimension(0, 0));
        booksTable.setSelectionBackground(new Color(232, 240, 254)); // Soft blue selection
        booksTable.setSelectionForeground(new Color(50, 50, 50));

        // 2. Header Styling
        JTableHeader header = booksTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(80, 80, 80));
        header.setBackground(Color.WHITE); // White header
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(230, 230, 230))); // Bottom border only
        header.setPreferredSize(new Dimension(header.getWidth(), 40)); // Taller header

        // 3. Alternating Row Colors (Zebra Striping)
        booksTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Add padding to cell content
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                if (!isSelected) {
                    c.setBackground(Color.WHITE); // Keep it simple/white
                } else {
                    c.setBackground(new Color(232, 240, 254));
                }
                return c;
            }
        });
    }
    //dsadadadsada
    private void customizeTableColumns() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Center ISBN and Availability
        if(booksTable.getColumnCount() >= 4) {
            booksTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ISBN
            booksTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Author
            booksTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); //Availability
        }
    }

    private JButton createModernButton(String text) {
        FancyHoverButton button = new FancyHoverButton(text);
        button.setFont(modernFont);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return button;
    }
    
    private void updateFilter() {
        String text = searchField.getText();
        if (rowSorter != null) {
            if (text.trim().length() == 0) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        }
    }
    
    // Helper to send POST and return the server's response as a String
    private String postDataWithResponse(String urlString, String urlParameters) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        
        // Write parameters
        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            out.writeBytes(urlParameters);
        }

        // Read response
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void showModernDialog(String title, String message, boolean isSuccess) {
        JDialog dialog = new JDialog(this, title, true); // Modal dialog
        dialog.setUndecorated(true); // Remove standard window borders for modern look
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        // Main Panel with Border
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        // Add a subtle border (Green for success, Red for error)
        Color borderColor = isSuccess ? new Color(76, 175, 80) : new Color(220, 53, 69);
        panel.setBorder(BorderFactory.createLineBorder(borderColor, 2));

        // --- Header ---
        JLabel headerLabel = new JLabel(isSuccess ? "✔ Success" : "⚠ Error");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerLabel.setForeground(borderColor);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        panel.add(headerLabel, BorderLayout.NORTH);

        // --- Message Body ---
        // Use HTML for automatic text wrapping
        JLabel msgLabel = new JLabel("<html><div style='text-align: center;'>" + message + "</div></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        msgLabel.setForeground(new Color(60, 60, 60));
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(msgLabel, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Reuse your FancyHoverButton
        FancyHoverButton okButton = new FancyHoverButton("OK");
        okButton.setPreferredSize(new Dimension(100, 40));
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        // Add Enter key support to close dialog
        dialog.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> dialog.dispose());

        btnPanel.add(okButton);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }
    //kkk
    private void checkoutBook() {
        JDialog dialog = new JDialog(this, "Checkout Book", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JTextField isbnField = new JTextField(20);
        JTextField borrowerIdField = new JTextField(20); 

        addFormRow(panel, gbc, 0, "ISBN:", isbnField);
        addFormRow(panel, gbc, 1, "Borrower ID:", borrowerIdField);

        JButton saveButton = createModernButton("Checkout");
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String isbn = isbnField.getText().trim();
            String borrowerId = borrowerIdField.getText().trim();

            if (isbn.isEmpty() ||  borrowerId.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required.");
                return;
            }

            sendToCheckout(isbn, borrowerId, lib.getLibID()); // Create this PHP

            dialog.dispose();
        });

        dialog.add(panel);
        dialog.setVisible(true);
    }
    //Checkout helper
    private void sendToCheckout(String isbn, String borrowerId, String libId) {
        new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/checkoutBook.php";
                String params = "isbn=" + URLEncoder.encode(isbn, "UTF-8") +
                                "&Card_id=" + URLEncoder.encode(borrowerId, "UTF-8") +
                                "&lib_id_checkout=" + URLEncoder.encode(libId, "UTF-8");

                // 1. Send Request and GET THE RESPONSE STRING
                String response = postDataWithResponse(urlString, params);

                // 2. Parse JSON
                JSONObject json = new JSONObject(response);
                String status = json.optString("status");
                String message = json.optString("message");

                // 3. Update UI on Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showModernDialog("Success", message, true);
                        loadAllBooks(); // Refresh table only on success
                    } else {
                        showModernDialog("Error", message, false);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    showModernDialog("Connection Error", e.getMessage(), false));
            }
        }).start();
    }

   
    static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        private final int THUMB_SIZE = 60;

        @Override
        protected Dimension getMaximumThumbSize() {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                return new Dimension(0, THUMB_SIZE);
            }
            return new Dimension(THUMB_SIZE, 0);
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                return new Dimension(0, THUMB_SIZE);
            }
            return new Dimension(THUMB_SIZE, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new JButton() {
                @Override public Dimension getPreferredSize() { return new Dimension(0, 0); }
            };
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new JButton() {
                @Override public Dimension getPreferredSize() { return new Dimension(0, 0); }
            };
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Transparent track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Grey rounded thumb
            g2.setColor(new Color(200, 200, 200));
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, 
                           thumbBounds.width - 4, thumbBounds.height - 4, 
                           8, 8);
            g2.dispose();
        }
    }
    
    // Main for testing
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception e) {}
        Librarian dummy = new Librarian("Test Lib", "test@test.com", "L001");
        SwingUtilities.invokeLater(() -> new ManageBooksDashboard(dummy).setVisible(true));
    }
}
