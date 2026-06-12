package com.smartclothing.ui;

import com.smartclothing.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class OrderForm extends JPanel {
    private JTabbedPane tabbedPane;

    // --- New Order Tab Components ---
    private JComboBox<CustomerItem> cmbCustomers;
    private JComboBox<ProductItem> cmbProducts;
    private JSpinner spinQty;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTotalAmount;
    private JButton btnAddToCart, btnRemoveFromCart, btnPlaceOrder;
    private double orderTotal = 0.0;

    // --- Order History Tab Components ---
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JTable itemsTable;
    private DefaultTableModel itemsModel;
    private JButton btnCancelOrder, btnRefreshHistory;

    public OrderForm() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        initNewOrderTab();
        initHistoryTab();

        add(tabbedPane, BorderLayout.CENTER);

        // Load Initial Data
        refreshNewOrderForm();
        loadOrderHistory();
    }

    // --- New Order Tab ---
    private void initNewOrderTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title and Customer Panel
        JPanel topSelectionPanel = new JPanel(new GridBagLayout());
        topSelectionPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Create New Order");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        topSelectionPanel.add(titleLabel, gbc);

        JLabel lblCust = new JLabel("Select Customer:");
        lblCust.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbCustomers = new JComboBox<>();
        cmbCustomers.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        topSelectionPanel.add(lblCust, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        topSelectionPanel.add(cmbCustomers, gbc);

        // Product Selection Panel
        JPanel prodPanel = new JPanel(new GridBagLayout());
        prodPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Add Product to Cart"));
        GridBagConstraints gbcProd = new GridBagConstraints();
        gbcProd.insets = new Insets(6, 10, 6, 10);
        gbcProd.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblProd = new JLabel("Product:");
        lblProd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbProducts = new JComboBox<>();
        cmbProducts.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblQty = new JLabel("Quantity:");
        lblQty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinQty = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        spinQty.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        btnAddToCart = new JButton("Add To Cart");
        btnAddToCart.setBackground(new Color(99, 102, 241));
        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddToCart.addActionListener(e -> addToCart());

        gbcProd.gridx = 0; gbcProd.gridy = 0; gbcProd.weightx = 0;
        prodPanel.add(lblProd, gbcProd);
        gbcProd.gridx = 1; gbcProd.weightx = 1.0;
        prodPanel.add(cmbProducts, gbcProd);

        gbcProd.gridx = 0; gbcProd.gridy = 1; gbcProd.weightx = 0;
        prodPanel.add(lblQty, gbcProd);
        gbcProd.gridx = 1; gbcProd.weightx = 1.0;
        prodPanel.add(spinQty, gbcProd);

        gbcProd.gridx = 0; gbcProd.gridy = 2; gbcProd.gridwidth = 2;
        prodPanel.add(btnAddToCart, gbcProd);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.add(topSelectionPanel, BorderLayout.NORTH);
        leftPanel.add(prodPanel, BorderLayout.CENTER);

        // Cart Table Panel
        JPanel cartPanel = new JPanel(new BorderLayout(10, 10));
        cartPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Cart Items"));

        String[] columns = {"Product ID", "Product Name", "Size", "Unit Price ($)", "Qty", "Subtotal ($)"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.setRowHeight(26);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        // Center values
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        cartTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        // Footer Actions for Cart
        JPanel footerCartPanel = new JPanel(new BorderLayout(10, 10));
        
        lblTotalAmount = new JLabel("Total: $0.00");
        lblTotalAmount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        footerCartPanel.add(lblTotalAmount, BorderLayout.WEST);

        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRemoveFromCart = new JButton("Remove Item");
        btnRemoveFromCart.addActionListener(e -> removeFromCart());

        btnPlaceOrder = new JButton("Place Order");
        btnPlaceOrder.setBackground(new Color(16, 185, 129)); // Emerald Green
        btnPlaceOrder.setForeground(Color.WHITE);
        btnPlaceOrder.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlaceOrder.addActionListener(e -> placeOrder());

        cartActions.add(btnRemoveFromCart);
        cartActions.add(btnPlaceOrder);
        footerCartPanel.add(cartActions, BorderLayout.EAST);
        cartPanel.add(footerCartPanel, BorderLayout.SOUTH);

        // Split Panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, cartPanel);
        splitPane.setDividerLocation(380);
        panel.add(splitPane, BorderLayout.CENTER);

        tabbedPane.addTab("New Order", panel);
    }

    // --- Order History Tab ---
    private void initHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left Side: Order list
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Order Logs");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRefreshHistory = new JButton("Refresh");
        btnRefreshHistory.addActionListener(e -> loadOrderHistory());
        btnCancelOrder = new JButton("Cancel Order");
        btnCancelOrder.setBackground(new Color(244, 63, 94));
        btnCancelOrder.setForeground(Color.WHITE);
        btnCancelOrder.addActionListener(e -> cancelSelectedOrder());

        buttonsPanel.add(btnRefreshHistory);
        buttonsPanel.add(btnCancelOrder);
        headerPanel.add(buttonsPanel, BorderLayout.EAST);
        leftPanel.add(headerPanel, BorderLayout.NORTH);

        String[] historyCols = {"Order ID", "Customer", "Date", "Total Amount ($)", "Status"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(28);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadOrderItems();
            }
        });

        JScrollPane historyScroll = new JScrollPane(historyTable);
        leftPanel.add(historyScroll, BorderLayout.CENTER);

        // Right Side: Order items details
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Order Line Items"));

        String[] itemCols = {"Product", "Size", "Quantity", "Price ($)", "Subtotal ($)"};
        itemsModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        itemsTable = new JTable(itemsModel);
        itemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemsTable.setRowHeight(26);
        itemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JScrollPane itemsScroll = new JScrollPane(itemsTable);
        rightPanel.add(itemsScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(500);
        panel.add(splitPane, BorderLayout.CENTER);

        tabbedPane.addTab("Order History", panel);
    }

    // --- Methods for placing/adding orders ---
    public void refreshNewOrderForm() {
        // Load Customers
        cmbCustomers.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, phone FROM customers ORDER BY name");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                cmbCustomers.addItem(new CustomerItem(rs.getInt("id"), rs.getString("name"), rs.getString("phone")));
            }
        } catch (SQLException e) {
            System.err.println("Error loading customers for combobox: " + e.getMessage());
        }

        // Load Products
        cmbProducts.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, category, size, price, stock_quantity FROM products ORDER BY name, size");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                cmbProducts.addItem(new ProductItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("size"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading products for combobox: " + e.getMessage());
        }
    }

    private void addToCart() {
        ProductItem selectedProd = (ProductItem) cmbProducts.getSelectedItem();
        if (selectedProd == null) return;

        int qty = (int) spinQty.getValue();
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if qty exceeds stock
        if (qty > selectedProd.stock) {
            JOptionPane.showMessageDialog(this, "Insufficient stock. Available stock: " + selectedProd.stock, "Stock Alert", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if product already exists in cart, if so, accumulate qty and subtotal
        int existingRow = -1;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            int cartProdId = (int) cartModel.getValueAt(i, 0);
            if (cartProdId == selectedProd.id) {
                existingRow = i;
                break;
            }
        }

        if (existingRow != -1) {
            int currentQty = (int) cartModel.getValueAt(existingRow, 4);
            int newQty = currentQty + qty;

            if (newQty > selectedProd.stock) {
                JOptionPane.showMessageDialog(this, "Accumulated cart quantity (" + newQty + ") exceeds available stock (" + selectedProd.stock + ").", "Stock Alert", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double subTotal = newQty * selectedProd.price;
            cartModel.setValueAt(newQty, existingRow, 4);
            cartModel.setValueAt(Double.parseDouble(String.format("%.2f", subTotal)), existingRow, 5);
        } else {
            double subTotal = qty * selectedProd.price;
            cartModel.addRow(new Object[]{
                    selectedProd.id,
                    selectedProd.name,
                    selectedProd.size,
                    selectedProd.price,
                    qty,
                    Double.parseDouble(String.format("%.2f", subTotal))
            });
        }

        recalculateCartTotal();
    }

    private void removeFromCart() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart to remove.", "Selection Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cartModel.removeRow(selectedRow);
        recalculateCartTotal();
    }

    private void recalculateCartTotal() {
        orderTotal = 0.0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            orderTotal += (double) cartModel.getValueAt(i, 5);
        }
        lblTotalAmount.setText("Total: $" + String.format("%.2f", orderTotal));
    }

    private void placeOrder() {
        CustomerItem customer = (CustomerItem) cmbCustomers.getSelectedItem();
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.", "Customer Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Your shopping cart is empty.", "Cart Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Establish database transaction to insert order and update stock
        Connection conn = null;
        PreparedStatement stmtOrder = null;
        PreparedStatement stmtItem = null;
        PreparedStatement stmtStock = null;
        ResultSet rsKeys = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Insert order
            String insertOrderQuery = "INSERT INTO orders (customer_id, total_amount, status) VALUES (?, ?, 'Pending')";
            stmtOrder = conn.prepareStatement(insertOrderQuery, Statement.RETURN_GENERATED_KEYS);
            stmtOrder.setInt(1, customer.id);
            stmtOrder.setDouble(2, orderTotal);
            stmtOrder.executeUpdate();

            rsKeys = stmtOrder.getGeneratedKeys();
            int orderId = 0;
            if (rsKeys.next()) {
                orderId = rsKeys.getInt(1);
            } else {
                throw new SQLException("Failed to retrieve generated order ID.");
            }

            // 2. Insert items and decrement stock
            String insertItemQuery = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
            stmtItem = conn.prepareStatement(insertItemQuery);

            String updateStockQuery = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";
            stmtStock = conn.prepareStatement(updateStockQuery);

            for (int i = 0; i < cartModel.getRowCount(); i++) {
                int prodId = (int) cartModel.getValueAt(i, 0);
                double price = (double) cartModel.getValueAt(i, 3);
                int qty = (int) cartModel.getValueAt(i, 4);

                // Add to order items batch
                stmtItem.setInt(1, orderId);
                stmtItem.setInt(2, prodId);
                stmtItem.setInt(3, qty);
                stmtItem.setDouble(4, price);
                stmtItem.addBatch();

                // Update stock batch
                stmtStock.setInt(1, qty);
                stmtStock.setInt(2, prodId);
                stmtStock.setInt(3, qty); // check constraint that stock doesn't drop below zero
                stmtStock.addBatch();
            }

            stmtItem.executeBatch();
            
            // Execute stock update and check if any product failed the stock check
            int[] stockResults = stmtStock.executeBatch();
            for (int res : stockResults) {
                if (res == 0) {
                    throw new SQLException("Stock update failed. A product may have gone out of stock while placing order.");
                }
            }

            conn.commit(); // Commit Transaction

            JOptionPane.showMessageDialog(this, 
                    "Order #" + orderId + " placed successfully!\nTotal Amount: $" + String.format("%.2f", orderTotal) + 
                    "\nPlease process the payment inside the Payments tab.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            // Reset checkout state
            cartModel.setRowCount(0);
            recalculateCartTotal();
            spinQty.setValue(1);
            
            refreshNewOrderForm();
            loadOrderHistory();

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction on failure
                } catch (SQLException rollbackEx) {
                    System.err.println("Transaction rollback failed: " + rollbackEx.getMessage());
                }
            }
            JOptionPane.showMessageDialog(this, "Failed to place order: " + ex.getMessage(), "Order Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rsKeys != null) rsKeys.close();
                if (stmtOrder != null) stmtOrder.close();
                if (stmtItem != null) stmtItem.close();
                if (stmtStock != null) stmtStock.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection objects: " + e.getMessage());
            }
        }
    }

    // --- Methods for Order History ---
    public void loadOrderHistory() {
        historyModel.setRowCount(0);
        itemsModel.setRowCount(0);

        String query = "SELECT o.id, c.name AS customer_name, o.order_date, o.total_amount, o.status " +
                       "FROM orders o " +
                       "LEFT JOIN customers c ON o.customer_id = c.id " +
                       "ORDER BY o.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                int id = rs.getInt("id");
                String customer = rs.getString("customer_name");
                if (customer == null) customer = "[Walk-in/Deleted]";
                Timestamp date = rs.getTimestamp("order_date");
                double total = rs.getDouble("total_amount");
                String status = rs.getString("status");

                historyModel.addRow(new Object[]{
                        id,
                        customer,
                        date != null ? sdf.format(date) : "",
                        total,
                        status
                });
            }
        } catch (SQLException e) {
            System.err.println("Error loading order history: " + e.getMessage());
        }
    }

    private void loadOrderItems() {
        itemsModel.setRowCount(0);
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) return;

        int orderId = (int) historyTable.getValueAt(selectedRow, 0);

        String query = "SELECT p.name AS prod_name, p.size, oi.quantity, oi.unit_price " +
                       "FROM order_items oi " +
                       "JOIN products p ON oi.product_id = p.id " +
                       "WHERE oi.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("prod_name");
                    String size = rs.getString("size");
                    int qty = rs.getInt("quantity");
                    double price = rs.getDouble("unit_price");
                    double subtotal = qty * price;

                    itemsModel.addRow(new Object[]{name, size, qty, price, Double.parseDouble(String.format("%.2f", subtotal))});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading order items: " + e.getMessage());
        }
    }

    private void cancelSelectedOrder() {
        int selectedRow = historyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order from the list to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int orderId = (int) historyTable.getValueAt(selectedRow, 0);
        String status = (String) historyTable.getValueAt(selectedRow, 4);

        if ("Cancelled".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This order is already cancelled.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to cancel Order #" + orderId + "?\nThis will restore stock quantities for the items.", 
                "Cancel Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        Connection conn = null;
        PreparedStatement stmtStatus = null;
        PreparedStatement stmtGetItems = null;
        PreparedStatement stmtRestoreStock = null;
        ResultSet rsItems = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Get the items in the order to restore their stock
            String selectItems = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
            stmtGetItems = conn.prepareStatement(selectItems);
            stmtGetItems.setInt(1, orderId);
            rsItems = stmtGetItems.executeQuery();

            String restoreStockQuery = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            stmtRestoreStock = conn.prepareStatement(restoreStockQuery);

            while (rsItems.next()) {
                int prodId = rsItems.getInt("product_id");
                int qty = rsItems.getInt("quantity");

                stmtRestoreStock.setInt(1, qty);
                stmtRestoreStock.setInt(2, prodId);
                stmtRestoreStock.addBatch();
            }
            stmtRestoreStock.executeBatch();

            // 2. Set order status to 'Cancelled'
            String updateStatus = "UPDATE orders SET status = 'Cancelled' WHERE id = ?";
            stmtStatus = conn.prepareStatement(updateStatus);
            stmtStatus.setInt(1, orderId);
            stmtStatus.executeUpdate();

            // 3. Mark payment status as failed/cancelled if it exists
            try (PreparedStatement stmtPay = conn.prepareStatement("UPDATE payments SET status = 'Failed' WHERE order_id = ?")) {
                stmtPay.setInt(1, orderId);
                stmtPay.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Order #" + orderId + " cancelled and stock levels restored.", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            refreshNewOrderForm();
            loadOrderHistory();
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException r) { System.err.println("Rollback error: " + r.getMessage()); }
            }
            JOptionPane.showMessageDialog(this, "Failed to cancel order: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rsItems != null) rsItems.close();
                if (stmtGetItems != null) stmtGetItems.close();
                if (stmtRestoreStock != null) stmtRestoreStock.close();
                if (stmtStatus != null) stmtStatus.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    // --- Helper Combobox Items ---
    private static class CustomerItem {
        int id;
        String name;
        String phone;

        public CustomerItem(int id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone != null ? phone : "N/A";
        }

        @Override
        public String toString() {
            return name + " (Phone: " + phone + ")";
        }
    }

    private static class ProductItem {
        int id;
        String name;
        String category;
        String size;
        double price;
        int stock;

        public ProductItem(int id, String name, String category, String size, double price, int stock) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.size = size;
            this.price = price;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return name + " [" + size + "] - $" + price + " (Stock: " + stock + ")";
        }
    }
}
