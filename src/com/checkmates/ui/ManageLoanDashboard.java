package com.checkmates.ui;

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

// PDF Imports
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.checkmates.ui.components.FancyHoverButton;
import com.checkmates.ui.components.FancyHoverButton2;

public class ManageLoanDashboard extends JFrame {

    private String cardId;
    private String borrowerName;
    private JTable recordsTable;
    private JTextField searchField;
    private TableRowSorter<TableModel> rowSorter;

    // Modern style properties
    private Color modernTextColor = new Color(60, 60, 60);
    private Font modernFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font modernTitleFont = new Font("Segoe UI", Font.BOLD, 24);
    private Color modernBackground = Color.WHITE;
    private Color modernPanelColor = new Color(240, 240, 240);
    private Color modernHighlightColor = new Color(200, 220, 255);

    public ManageLoanDashboard() {
        
        setTitle("Loan History for " + borrowerName);
        setSize(1100, 700); // Made wider for extra column
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setupLookAndFeel();
        initComponents();
        loadLoanHistory();
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
        JLabel headerLabel = new JLabel("Loan History Management", SwingConstants.LEFT);
        headerLabel.setFont(modernTitleFont);
        headerLabel.setForeground(modernTextColor);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // --- Table Setup ---
        recordsTable = new JTable();
        customizeTable(); // Apply modern styling

        JScrollPane scrollPane = new JScrollPane(recordsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel (Search & Buttons) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Search Bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel("Filter History: "));
        searchField = new JTextField(20);
        searchPanel.add(searchField);
        
        // Search Listener
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFilter(); }
        });

        // Buttons
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonContainer.setBackground(Color.WHITE);

        FancyHoverButton2 exportButton = new FancyHoverButton2("Export to PDF");
        exportButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exportButton.addActionListener(e -> exportTableToPDF());
        buttonContainer.add(exportButton);
        
        FancyHoverButton closeButton = new FancyHoverButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        closeButton.addActionListener(e -> dispose());
        buttonContainer.add(closeButton);

        bottomPanel.add(searchPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonContainer, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);
    }

    // --- Data Loading ---

    private void loadLoanHistory() {
        new Thread(() -> {
            try {
                // Correct URL for getting ALL loans (not just one borrower)
                String urlString = "http://cm8tes.com/CS4347_Project_Folder/getLoans.php";
                String response = fetchUrl(urlString);
                //System.out.println("JSON RECEIVED: " + response);

                updateTableWithJson(response);
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error loading loan: " + ex.getMessage()));
            }
        }).start();
    }

    private void updateTableWithJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            if ("success".equalsIgnoreCase(json.optString("status"))) {
                JSONArray recordsArray = json.getJSONArray("loans");
                
                // Updated Columns to include "Borrower Name" (Bname) from your PHP
                String[] columnNames = {"No.", "Isbn", "Card ID", "Date Out", "Due Date", "Date In", "Lib ID OUT", "Lib ID IN"};
                
                ArrayList<String[]> rowData = new ArrayList<>();
                for (int i = 0; i < recordsArray.length(); i++) {
                    String displayNo = String.valueOf(i + 1);
                    JSONObject obj = recordsArray.getJSONObject(i);
                    
                    String isbn = obj.optString("Isbn");
                    String cId = obj.optString("Card_id");
                    String dateOut = obj.optString("Date_out");
                    String dueDate = obj.optString("Due_date");
                    String dateIn = obj.optString("Date_in");
                    
                    if(dateIn == null || dateIn.equals("null")) dateIn = "---";
                    
                    String libOut = obj.optString("lib_id_checkout");
                    String libIn = obj.optString("lib_id_return");
                    if(libIn == null || libIn.equals("null")) libIn = "---";

                    // Add ALL fields to the row
                    rowData.add(new String[]{displayNo, isbn, cId, dateOut, dueDate, dateIn, libOut, libIn});
                }

                String[][] data = rowData.toArray(new String[0][]);
                
                SwingUtilities.invokeLater(() -> {
                    DefaultTableModel model = new DefaultTableModel(data, columnNames) {
                        @Override
                        public boolean isCellEditable(int row, int column) { return false; }
                    };
                    recordsTable.setModel(model);
                    
                    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                    centerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
                    for (int i = 0; i < recordsTable.getColumnCount(); i++) {
                        recordsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
                    }

                    rowSorter = new TableRowSorter<>(model);
                    recordsTable.setRowSorter(rowSorter);
                });
            } else {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "Error: " + json.optString("message"), "Error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    // --- Styling & Helpers ---

    private void customizeTable() {
        recordsTable.setFont(modernFont);
        recordsTable.setForeground(modernTextColor);
        recordsTable.setRowHeight(35); 
        recordsTable.setSelectionBackground(modernHighlightColor);
        recordsTable.setSelectionForeground(modernTextColor);
        recordsTable.setGridColor(new Color(230, 230, 230));
        recordsTable.setShowVerticalLines(false);
        recordsTable.setShowHorizontalLines(true);
        recordsTable.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = recordsTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(80, 80, 80));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(230, 230, 230)));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        recordsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                } else {
                    c.setBackground(modernHighlightColor);
                }
                return c;
            }
        });
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

    private JButton createModernButton(String text) {
        FancyHoverButton button = new FancyHoverButton(text);
        button.setFont(modernFont);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return button;
    }

    private void exportTableToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Loan History PDF");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".pdf");
            }
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                document.open();

                PdfPTable pdfTable = new PdfPTable(recordsTable.getColumnCount());
                PdfPCell headerCell = new PdfPCell(new Phrase("Loan History Report"));
                headerCell.setColspan(recordsTable.getColumnCount());
                headerCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                pdfTable.addCell(headerCell);

                for (int i = 0; i < recordsTable.getColumnCount(); i++) {
                    pdfTable.addCell(new Phrase(recordsTable.getColumnName(i)));
                }

                for (int rowIndex = 0; rowIndex < recordsTable.getRowCount(); rowIndex++) {
                    int modelRow = recordsTable.convertRowIndexToModel(rowIndex);
                    for (int col = 0; col < recordsTable.getColumnCount(); col++) {
                        Object val = recordsTable.getModel().getValueAt(modelRow, col);
                        pdfTable.addCell(new Phrase(val == null ? "" : val.toString()));
                    }
                }

                document.add(pdfTable);
                document.close();
                JOptionPane.showMessageDialog(this, "PDF exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManageLoanDashboard().setVisible(true));
    }
}