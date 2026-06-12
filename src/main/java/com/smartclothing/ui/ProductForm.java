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

public class ProductForm extends JPanel {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private JButton btnAdd, btnEdit, btnDelete, btnRefresh;

    public ProductForm() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Initialize UI components
        initComponents();
        loadCategories();
        loadProductData();
    }

    private void initComponents() {
        // --- Header Panel (Title + Actions) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Product Inventory");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Action Buttons Panel
        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setOpaque(false);

        btnAdd = new JButton("Add Product");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.setBackground(new Color(99, 102, 241)); // Accent Indigo
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> showProductDialog(null));

        btnEdit = new JButton("Edit");
        btnEdit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnEdit.addActionListener(e -> editSelectedProduct());

        btnDelete = new JButton("Delete");
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnDelete.setBackground(new Color(244, 63, 94)); // Danger Rose
        btnDelete.setForeground(Color.WHITE);
        btnDelete.addActionListener(e -> deleteSelectedProduct());

        btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.addActionListener(e -> loadProductData());

        actionButtonPanel.add(btnAdd);
        actionButtonPanel.add(btnEdit);
        actionButtonPanel.add(btnDelete);
        actionButtonPanel.add(btnRefresh);
        headerPanel.add(actionButtonPanel, BorderLayout.EAST);

        // --- Filter Panel (Search & Dropdowns) ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name...");
        searchField.addActionListener(e -> loadProductData());

        categoryFilter = new JComboBox<>();
        categoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryFilter.addActionListener(e -> loadProductData());

        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSearch.addActionListener(e -> loadProductData());

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Category:"));
        filterPanel.add(categoryFilter);
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
        String[] columns = {"ID", "Name", "Category", "Size", "Price ($)", "Stock Qty", "Min Level", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productTable.setRowHeight(30);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setShowGrid(true);
        productTable.setGridColor(new Color(230, 230, 230, 50));

        // Custom Cell Renderer for Status & Row styling
        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                int stock = (int) table.getValueAt(row, 5);
                int minLevel = (int) table.getValueAt(row, 6);

                if (!isSelected) {
                    if (stock == 0) {
                        c.setBackground(new Color(254, 226, 226)); // Soft red for out of stock
                        c.setForeground(new Color(220, 38, 38));
                    } else if (stock <= minLevel) {
                        c.setBackground(new Color(254, 243, 199)); // Soft orange/yellow for low stock
                        c.setForeground(new Color(217, 119, 6));
                    } else {
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }

                // Center align Status, ID, Size, Price, Qty, Min Level
                if (column == 0 || column == 3 || column == 4 || column == 5 || column == 6 || column == 7) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadCategories() {
        categoryFilter.removeAllItems();
        categoryFilter.addItem("All Categories");
        
        String query = "SELECT DISTINCT category FROM products ORDER BY category";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categoryFilter.addItem(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
    }

    public void loadProductData() {
        tableModel.setRowCount(0);
        String searchText = searchField.getText().trim();
        String selectedCategory = (String) categoryFilter.getSelectedItem();

        StringBuilder query = new StringBuilder("SELECT * FROM products WHERE 1=1");
        if (!searchText.isEmpty()) {
            query.append(" AND LOWER(name) LIKE LOWER(?)");
        }
        if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
            query.append(" AND category = ?");
        }
        query.append(" ORDER BY id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            int paramIndex = 1;
            if (!searchText.isEmpty()) {
                stmt.setString(paramIndex++, "%" + searchText + "%");
            }
            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                stmt.setString(paramIndex, selectedCategory);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String category = rs.getString("category");
                    String size = rs.getString("size");
                    double price = rs.getDouble("price");
                    int stock = rs.getInt("stock_quantity");
                    int minLevel = rs.getInt("min_stock_level");
                    
                    String status = "In Stock";
                    if (stock == 0) {
                        status = "Out of Stock";
                    } else if (stock <= minLevel) {
                        status = "Low Stock";
                    }

                    tableModel.addRow(new Object[]{id, name, category, size, price, stock, minLevel, status});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading product data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productTable.getValueAt(selectedRow, 0);
        showProductDialog(productId);
    }

    private void deleteSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) productTable.getValueAt(selectedRow, 0);
        String productName = (String) productTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete product '" + productName + "'?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM products WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, productId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Product deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadCategories();
                loadProductData();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showProductDialog(Integer productId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                productId == null ? "Add Product" : "Edit Product", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 450);
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels and Fields
        JLabel lblName = new JLabel("Product Name:");
        JTextField txtName = new JTextField(20);
        
        JLabel lblCategory = new JLabel("Category:");
        // Editable combo box so users can select existing or type a new one
        JComboBox<String> cmbCategory = new JComboBox<>();
        cmbCategory.setEditable(true);
        // Populate categories
        for (int i = 1; i < categoryFilter.getItemCount(); i++) {
            cmbCategory.addItem(categoryFilter.getItemAt(i));
        }

        JLabel lblSize = new JLabel("Size (e.g. S, M, L, XL, 32):");
        JTextField txtSize = new JTextField(10);

        JLabel lblPrice = new JLabel("Price ($):");
        JSpinner spinPrice = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 100000.00, 1.00));
        JSpinner.NumberEditor priceEditor = new JSpinner.NumberEditor(spinPrice, "$#,##0.00");
        spinPrice.setEditor(priceEditor);

        JLabel lblStock = new JLabel("Stock Quantity:");
        JSpinner spinStock = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));

        JLabel lblMinLevel = new JLabel("Min Stock Level:");
        JSpinner spinMinLevel = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));

        // Load data if editing
        if (productId != null) {
            String selectQuery = "SELECT * FROM products WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        txtName.setText(rs.getString("name"));
                        cmbCategory.setSelectedItem(rs.getString("category"));
                        txtSize.setText(rs.getString("size"));
                        spinPrice.setValue(rs.getDouble("price"));
                        spinStock.setValue(rs.getInt("stock_quantity"));
                        spinMinLevel.setValue(rs.getInt("min_stock_level"));
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Error loading product info: " + e.getMessage());
            }
        }

        // Layout Components
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblName, gbc);
        gbc.gridx = 1; dialog.add(txtName, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblCategory, gbc);
        gbc.gridx = 1; dialog.add(cmbCategory, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblSize, gbc);
        gbc.gridx = 1; dialog.add(txtSize, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblPrice, gbc);
        gbc.gridx = 1; dialog.add(spinPrice, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblStock, gbc);
        gbc.gridx = 1; dialog.add(spinStock, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; dialog.add(lblMinLevel, gbc);
        gbc.gridx = 1; dialog.add(spinMinLevel, gbc);

        // Action Buttons inside Dialog
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

        // Button Handlers
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String category = (cmbCategory.getSelectedItem() != null) ? cmbCategory.getSelectedItem().toString().trim() : "";
            String size = txtSize.getText().trim();
            double price = (double) spinPrice.getValue();
            int stock = (int) spinStock.getValue();
            int minLevel = (int) spinMinLevel.getValue();

            if (name.isEmpty() || category.isEmpty() || size.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all text fields.", "Validation Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt;
                if (productId == null) {
                    String insertQuery = "INSERT INTO products (name, category, size, price, stock_quantity, min_stock_level) VALUES (?, ?, ?, ?, ?, ?)";
                    stmt = conn.prepareStatement(insertQuery);
                } else {
                    String updateQuery = "UPDATE products SET name = ?, category = ?, size = ?, price = ?, stock_quantity = ?, min_stock_level = ? WHERE id = ?";
                    stmt = conn.prepareStatement(updateQuery);
                    stmt.setInt(7, productId);
                }

                stmt.setString(1, name);
                stmt.setString(2, category);
                stmt.setString(3, size);
                stmt.setDouble(4, price);
                stmt.setInt(5, stock);
                stmt.setInt(6, minLevel);

                stmt.executeUpdate();
                stmt.close();
                conn.close();

                JOptionPane.showMessageDialog(dialog, "Product saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadCategories();
                loadProductData();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error saving product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }
}
