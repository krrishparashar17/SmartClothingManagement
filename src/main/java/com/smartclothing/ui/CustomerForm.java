package com.smartclothing.ui;

import com.smartclothing.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerForm extends JPanel {
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton btnAdd, btnEdit, btnDelete, btnRefresh;

    public CustomerForm() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        loadCustomerData();
    }

    private void initComponents() {
        // --- Header Panel (Title + Actions) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Customer Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Action Buttons Panel
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setOpaque(false);

        btnAdd = new JButton("Add Customer");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.setBackground(new Color(99, 102, 241)); // Accent Indigo
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> showCustomerDialog(null));

        btnEdit = new JButton("Edit");
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnEdit.addActionListener(e -> editSelectedCustomer());

        btnDelete = new JButton("Delete");
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnDelete.setBackground(new Color(244, 63, 94)); // Danger Rose
        btnDelete.setForeground(Color.WHITE);
        btnDelete.addActionListener(e -> deleteSelectedCustomer());

        btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.addActionListener(e -> loadCustomerData());

        actionButtonPanel.add(btnAdd);
        actionButtonPanel.add(btnEdit);
        actionButtonPanel.add(btnDelete);
        actionButtonPanel.add(btnRefresh);
        headerPanel.add(actionButtonPanel, BorderLayout.EAST);

        // --- Filter Panel (Search bar) ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setOpaque(false);

        searchField = new JTextField(25);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, email, or phone...");
        searchField.addActionListener(e -> loadCustomerData());

        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSearch.addActionListener(e -> loadCustomerData());

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(btnSearch);

        // Assemble Top Section
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(headerPanel);
        topPanel.add(Box.createVerticalStrut(15));
        topPanel.add(filterPanel);
        add(topPanel, BorderLayout.NORTH);

        // --- Table Panel ---
        String[] columns = {"ID", "Name", "Email", "Phone", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerTable.setRowHeight(30);
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.setShowGrid(true);
        customerTable.setGridColor(new Color(230, 230, 230, 50));

        // Custom Cell Renderer to center ID and phone
        customerTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (column == 0 || column == 3) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                if (!isSelected) {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                } else {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(customerTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadCustomerData() {
        tableModel.setRowCount(0);
        String searchText = searchField.getText().trim();

        StringBuilder query = new StringBuilder("SELECT * FROM customers");
        if (!searchText.isEmpty()) {
            query.append(" WHERE LOWER(name) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) OR phone LIKE ?");
        }
        query.append(" ORDER BY id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    String address = rs.getString("address");

                    tableModel.addRow(new Object[]{id, name, email != null ? email : "", phone != null ? phone : "", address != null ? address : ""});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customer data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) customerTable.getValueAt(selectedRow, 0);
        showCustomerDialog(id);
    }

    private void deleteSelectedCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) customerTable.getValueAt(selectedRow, 0);
        String customerName = (String) customerTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete customer '" + customerName + "'?\nThis will set their associated orders customer reference to null.", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM customers WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Customer deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadCustomerData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting customer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCustomerDialog(Integer customerId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                customerId == null ? "Add Customer" : "Edit Customer", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = new JLabel("Full Name:");
        JTextField txtName = new JTextField(20);

        JLabel lblEmail = new JLabel("Email Address:");
        JTextField txtEmail = new JTextField(20);

        JLabel lblPhone = new JLabel("Phone Number:");
        JTextField txtPhone = new JTextField(20);

        JLabel lblAddress = new JLabel("Billing/Shipping Address:");
        JTextArea txtAddress = new JTextArea(4, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane addrScroll = new JScrollPane(txtAddress);

        // Load details if editing
        if (customerId != null) {
            String selectQuery = "SELECT * FROM customers WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        txtName.setText(rs.getString("name"));
                        txtEmail.setText(rs.getString("email"));
                        txtPhone.setText(rs.getString("phone"));
                        txtAddress.setText(rs.getString("address"));
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Error loading customer info: " + e.getMessage());
            }
        }

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblName, gbc);
        gbc.gridx = 1; dialog.add(txtName, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblEmail, gbc);
        gbc.gridx = 1; dialog.add(txtEmail, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblPhone, gbc);
        gbc.gridx = 1; dialog.add(txtPhone, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblAddress, gbc);
        gbc.gridx = 1; dialog.add(addrScroll, gbc);

        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSave = new JButton("Save");
        btnSave.setBackground(new Color(99, 102, 241));
        btnSave.setForeground(Color.WHITE);
        JButton btnCancel = new JButton("Cancel");

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 12, 10, 12);
        dialog.add(btnPanel, gbc);

        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String phone = txtPhone.getText().trim();
            String address = txtAddress.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Full Name is required.", "Validation Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt;
                if (customerId == null) {
                    String insertQuery = "INSERT INTO customers (name, email, phone, address) VALUES (?, ?, ?, ?)";
                    stmt = conn.prepareStatement(insertQuery);
                } else {
                    String updateQuery = "UPDATE customers SET name = ?, email = ?, phone = ?, address = ? WHERE id = ?";
                    stmt = conn.prepareStatement(updateQuery);
                    stmt.setInt(5, customerId);
                }

                stmt.setString(1, name);
                stmt.setString(2, email.isEmpty() ? null : email);
                stmt.setString(3, phone.isEmpty() ? null : phone);
                stmt.setString(4, address.isEmpty() ? null : address);

                stmt.executeUpdate();
                stmt.close();
                conn.close();

                JOptionPane.showMessageDialog(dialog, "Customer profile saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadCustomerData();
            } catch (SQLException ex) {
                if (ex.getMessage().contains("customers_email_key")) {
                    JOptionPane.showMessageDialog(dialog, "This email address is already in use.", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Error saving customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dialog.setVisible(true);
    }
}
