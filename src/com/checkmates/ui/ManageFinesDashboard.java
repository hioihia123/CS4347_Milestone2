package com.checkmates.ui;

import com.checkmates.model.Librarian;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.checkmates.ui.components.FancyHoverButton;
import com.checkmates.ui.components.FancyHoverButton2;

/**
 * FR5: Fines Management Dashboard
 * Displays fines grouped by borrower (card_no) with ability to:
 * - Refresh/update fines
 * - Pay fines (only for returned books, no partial payments)
 * - Filter paid/unpaid fines
 */
public class ManageFinesDashboard extends JFrame {

    private JTable finesTable;
    private JCheckBox showPaidCheckBox;
    private TableRowSorter<TableModel> rowSorter;
    private Librarian lib;

    // Modern style properties
    private Color modernTextColor = new Color(60, 60, 60);
    private Font modernFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font modernTitleFont = new Font("Segoe UI", Font.BOLD, 24);
    private Color modernBackground = Color.WHITE;
    private Color modernPanelColor = new Color(240, 240, 240);
    private Color modernHighlightColor = new Color(200, 220, 255);

    public ManageFinesDashboard(Librarian libObj) {
        this.lib = libObj;
        setTitle("Fines Management");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupLookAndFeel();
        initComponents();
        loadFines(false); // Load unpaid fines by default
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

        // --- Header ---
        JLabel headerLabel = new JLabel("Fines Management", SwingConstants.LEFT);
        headerLabel.setFont(modernTitleFont);
        headerLabel.setForeground(modernTextColor);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Table Setup ---
        finesTable = new JTable();
        customizeTable();

        JScrollPane scrollPane = new JScrollPane(finesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel (Filter & Buttons) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(Color.WHITE);
        showPaidCheckBox = new JCheckBox("Show Paid Fines");
        showPaidCheckBox.setFont(modernFont);
        showPaidCheckBox.setSelected(false); // Default: hide paid fines
        showPaidCheckBox.addActionListener(e -> {
            boolean showPaid = showPaidCheckBox.isSelected();
            loadFines(showPaid);
        });
        filterPanel.add(showPaidCheckBox);

        // Buttons Panel
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonContainer.setBackground(Color.WHITE);

        FancyHoverButton refreshButton = new FancyHoverButton("Refresh Fines");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        refreshButton.addActionListener(e -> refreshFines());
        buttonContainer.add(refreshButton);

        FancyHoverButton2 payButton = new FancyHoverButton2("Pay Selected Borrower");
        payButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        payButton.addActionListener(e -> paySelectedFines());
        buttonContainer.add(payButton);

        FancyHoverButton closeButton = new FancyHoverButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeButton.addActionListener(e -> dispose());
        buttonContainer.add(closeButton);

        bottomPanel.add(filterPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonContainer, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);
    }

    // --- Data Loading ---

    /**
     * Loads fines from the backend, grouped by borrower (card_no)
     * @param includePaid Whether to include paid fines in the results
     */
    private void loadFines(boolean includePaid) {
        new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/getFines.php";
                String params = "include_paid=" + (includePaid ? "1" : "0");
                String response = postDataWithResponse(urlString, params);
                updateTableWithJson(response);
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Error loading fines: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Updates the fines table with JSON data from backend
     * Data should be grouped by card_no with SUM of fine_amt
     */
    private void updateTableWithJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            if ("success".equalsIgnoreCase(json.optString("status"))) {
                JSONArray finesArray = json.getJSONArray("fines");

                // Columns: Card ID, Borrower Name, Total Fine Amount, Paid Status, Loan IDs (hidden)
                String[] columnNames = {"Card ID", "Borrower Name", "Total Fine ($)", "Status", "Hidden_Loan_Ids"};

                ArrayList<String[]> rowData = new ArrayList<>();
                for (int i = 0; i < finesArray.length(); i++) {
                    JSONObject obj = finesArray.getJSONObject(i);
                    String cardId = obj.optString("Card_id");
                    String borrowerName = obj.optString("Bname");
                    String totalFine = obj.optString("Total_fine");
                    String paid = obj.optString("Paid"); // "1" or "0"
                    String loanIds = obj.optString("Loan_ids"); // Comma-separated loan IDs

                    String status = "1".equals(paid) ? "Paid" : "Unpaid";
                    String formattedFine = String.format("$%.2f", Double.parseDouble(totalFine));

                    rowData.add(new String[]{cardId, borrowerName, formattedFine, status, loanIds});
                }

                String[][] data = rowData.toArray(new String[0][]);

                SwingUtilities.invokeLater(() -> {
                    DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    finesTable.setModel(model);

                    // Hide the Loan IDs column
                    finesTable.removeColumn(finesTable.getColumnModel().getColumn(4));

                    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
                    for (int i = 0; i < finesTable.getColumnCount(); i++) {
                        finesTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
                    }

                    // Color code paid vs unpaid rows
                    finesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value,
                                                                     boolean isSelected, boolean hasFocus,
                                                                     int row, int column) {
                            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                            // Get status from column 3
                            String status = (String) table.getModel().getValueAt(row, 3);
                            if (!isSelected) {
                                if ("Paid".equals(status)) {
                                    c.setBackground(new Color(220, 255, 220)); // Light green for paid
                                } else {
                                    c.setBackground(Color.WHITE);
                                }
                            } else {
                                c.setBackground(modernHighlightColor);
                            }
                            return c;
                        }
                    });

                    rowSorter = new TableRowSorter<>(model);
                    finesTable.setRowSorter(rowSorter);
                });
            } else {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Error: " + json.optString("message"),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * FR5: Refreshes/updates fines in the FINES table
     * This simulates what a cron job would do daily
     */
    private void refreshFines() {
        new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/updateFines.php";
                String response = postDataWithResponse(urlString, "");
                
                JSONObject json = new JSONObject(response);
                String status = json.optString("status");
                String message = json.optString("message");

                SwingUtilities.invokeLater(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showModernDialog("Success", message, true);
                        // Reload fines after update
                        loadFines(showPaidCheckBox.isSelected());
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

    /**
     * FR5: Pays fines for selected borrower
     * Rules:
     * - Cannot pay fines for books not yet returned
     * - Cannot pay partial fines - must pay all fines for borrower
     * - Sets paid = TRUE for all fines of that borrower
     */
    private void paySelectedFines() {
        int selectedRow = finesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a borrower to pay fines.");
            return;
        }

        int modelRow = finesTable.convertRowIndexToModel(selectedRow);
        String cardId = (String) finesTable.getModel().getValueAt(modelRow, 0);
        String borrowerName = (String) finesTable.getModel().getValueAt(modelRow, 1);
        String totalFine = (String) finesTable.getModel().getValueAt(modelRow, 2);
        String status = (String) finesTable.getModel().getValueAt(modelRow, 3);
        String loanIds = (String) finesTable.getModel().getValueAt(modelRow, 4); // Hidden column

        // Check if already paid
        if ("Paid".equals(status)) {
            JOptionPane.showMessageDialog(this, "Fines for this borrower are already paid.");
            return;
        }

        // Confirm payment
        int confirm = JOptionPane.showConfirmDialog(this,
            "Pay all fines for " + borrowerName + "?\nTotal Amount: " + totalFine,
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            sendPayment(cardId, loanIds);
        }
    }

    /**
     * Sends payment request to backend
     */
    private void sendPayment(String cardId, String loanIds) {
        new Thread(() -> {
            try {
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/payFines.php";
                String params = "card_id=" + URLEncoder.encode(cardId, "UTF-8")
                              + "&loan_ids=" + URLEncoder.encode(loanIds, "UTF-8");

                String response = postDataWithResponse(urlString, params);

                JSONObject json = new JSONObject(response);
                String status = json.optString("status");
                String message = json.optString("message");

                SwingUtilities.invokeLater(() -> {
                    if ("success".equalsIgnoreCase(status)) {
                        showModernDialog("Success", message, true);
                        // Reload fines after payment
                        loadFines(showPaidCheckBox.isSelected());
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

    // --- Styling & Helpers ---

    private void customizeTable() {
        finesTable.setFont(modernFont);
        finesTable.setForeground(modernTextColor);
        finesTable.setRowHeight(35);
        finesTable.setSelectionBackground(modernHighlightColor);
        finesTable.setSelectionForeground(modernTextColor);
        finesTable.setGridColor(new Color(230, 230, 230));
        finesTable.setShowVerticalLines(false);
        finesTable.setShowHorizontalLines(true);
        finesTable.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = finesTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(80, 80, 80));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(230, 230, 230)));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
    }

    private void showModernDialog(String title, String message, boolean isSuccess) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        Color borderColor = isSuccess ? new Color(76, 175, 80) : new Color(220, 53, 69);
        panel.setBorder(BorderFactory.createLineBorder(borderColor, 2));

        JLabel headerLabel = new JLabel(isSuccess ? "✔ Success" : "⚠ Error");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerLabel.setForeground(borderColor);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        panel.add(headerLabel, BorderLayout.NORTH);

        JLabel msgLabel = new JLabel("<html><div style='text-align: center;'>" + message + "</div></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        msgLabel.setForeground(new Color(60, 60, 60));
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        panel.add(msgLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        FancyHoverButton okButton = new FancyHoverButton("OK");
        okButton.setPreferredSize(new Dimension(100, 40));
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dialog.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(e -> dialog.dispose());

        btnPanel.add(okButton);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private String postDataWithResponse(String urlString, String params) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        if (params != null && !params.isEmpty()) {
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    public static void main(String[] args) {
        Librarian dummy = new Librarian("Test Lib", "test@test.com", "L001");
        SwingUtilities.invokeLater(() -> new ManageFinesDashboard(dummy).setVisible(true));
    }
}

