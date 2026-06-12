package com.smartclothing.ui;

import com.smartclothing.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class PaymentForm extends JPanel {
    private JTabbedPane tabbedPane;

    // --- Process Payment Tab ---
    private JTable pendingTable;
    private DefaultTableModel pendingModel;
    private JComboBox<String> cmbPaymentMethod;
    private JSpinner spinAmountPaid;
    private JLabel lblSelectedOrder;
    private JButton btnProcessPayment, btnRefreshPending;
    private int selectedOrderId = -1;
    private double selectedOrderAmount = 0.0;

    // --- Payment History Tab ---
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JButton btnRefreshHistory;

    public PaymentForm() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        initProcessTab();
        initHistoryTab();

        add(tabbedPane, BorderLayout.CENTER);

        // Load data
        loadPendingOrders();
        loadPaymentHistory();
    }

    // --- Process Payments Tab ---
    private void initProcessTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left Side: Pending Orders
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Pending Invoices");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        btnRefreshPending = new JButton("Refresh");
        btnRefreshPending.addActionListener(e -> loadPendingOrders());
        headerPanel.add(btnRefreshPending, BorderLayout.EAST);
        leftPanel.add(headerPanel, BorderLayout.NORTH);

        String[] pendingCols = {"Order ID", "Customer", "Date", "Total Amount ($)"};
        pendingModel = new DefaultTableModel(pendingCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        pendingTable = new JTable(pendingModel);
        pendingTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pendingTable.setRowHeight(28);
        pendingTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        pendingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectOrderForPayment();
            }
        });

        // Center totals column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        pendingTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        pendingTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        pendingTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(pendingTable);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Right Side: Process Console
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Payment Processing Console"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblSelectedOrder = new JLabel("Please select a pending order...");
        lblSelectedOrder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblSelectedOrder.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        rightPanel.add(lblSelectedOrder, gbc);

        JLabel lblMethod = new JLabel("Payment Method:");
        lblMethod.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPaymentMethod = new JComboBox<>(new String[]{"Card", "UPI", "Cash"});
        cmbPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPaymentMethod.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        rightPanel.add(lblMethod, gbc);
        gbc.gridx = 1;
        rightPanel.add(cmbPaymentMethod, gbc);

        JLabel lblAmt = new JLabel("Amount Paid ($):");
        lblAmt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinAmountPaid = new JSpinner(new SpinnerNumberModel(0.00, 0.00, 100000.00, 1.00));
        spinAmountPaid.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinAmountPaid.setEnabled(false);
        gbc.gridx = 0; gbc.gridy = 2;
        rightPanel.add(lblAmt, gbc);
        gbc.gridx = 1;
        rightPanel.add(spinAmountPaid, gbc);

        btnProcessPayment = new JButton("Process Checkout");
        btnProcessPayment.setBackground(new Color(16, 185, 129)); // Emerald Green
        btnProcessPayment.setForeground(Color.WHITE);
        btnProcessPayment.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnProcessPayment.setEnabled(false);
        btnProcessPayment.addActionListener(e -> processCheckout());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 12, 10, 12);
        rightPanel.add(btnProcessPayment, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(480);
        panel.add(splitPane, BorderLayout.CENTER);

        tabbedPane.addTab("Process Payment", panel);
    }

    // --- Payment History Tab ---
    private void initHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Transaction History Logs");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        btnRefreshHistory = new JButton("Refresh Logs");
        btnRefreshHistory.addActionListener(e -> loadPaymentHistory());
        headerPanel.add(btnRefreshHistory, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] historyCols = {"Receipt ID", "Order ID", "Customer Name", "Payment Date", "Amount Paid ($)", "Method", "Status"};
        historyModel = new DefaultTableModel(historyCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(28);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Center alignments
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Payment History", panel);
    }

    // --- Loading and Actions ---
    public void loadPendingOrders() {
        pendingModel.setRowCount(0);
        resetConsole();

        String query = "SELECT o.id, c.name AS customer_name, o.order_date, o.total_amount " +
                       "FROM orders o " +
                       "LEFT JOIN customers c ON o.customer_id = c.id " +
                       "WHERE o.status = 'Pending' " +
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

                pendingModel.addRow(new Object[]{id, customer, date != null ? sdf.format(date) : "", total});
            }
        } catch (SQLException e) {
            System.err.println("Error loading pending orders: " + e.getMessage());
        }
    }

    public void loadPaymentHistory() {
        historyModel.setRowCount(0);

        String query = "SELECT p.id AS payment_id, p.order_id, c.name AS customer_name, p.payment_date, p.amount, p.payment_method, p.status " +
                       "FROM payments p " +
                       "JOIN orders o ON p.order_id = o.id " +
                       "LEFT JOIN customers c ON o.customer_id = c.id " +
                       "ORDER BY p.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                int payId = rs.getInt("payment_id");
                int orderId = rs.getInt("order_id");
                String customer = rs.getString("customer_name");
                if (customer == null) customer = "[Walk-in/Deleted]";
                Timestamp date = rs.getTimestamp("payment_date");
                double amount = rs.getDouble("amount");
                String method = rs.getString("payment_method");
                String status = rs.getString("status");

                historyModel.addRow(new Object[]{payId, orderId, customer, date != null ? sdf.format(date) : "", amount, method, status});
            }
        } catch (SQLException e) {
            System.err.println("Error loading payment history: " + e.getMessage());
        }
    }

    private void selectOrderForPayment() {
        int selectedRow = pendingTable.getSelectedRow();
        if (selectedRow == -1) {
            resetConsole();
            return;
        }

        selectedOrderId = (int) pendingTable.getValueAt(selectedRow, 0);
        selectedOrderAmount = (double) pendingTable.getValueAt(selectedRow, 3);

        lblSelectedOrder.setText("Selected Order #" + selectedOrderId + " - Total due: $" + String.format("%.2f", selectedOrderAmount));
        lblSelectedOrder.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSelectedOrder.setForeground(new Color(99, 102, 241)); // Indigo theme

        cmbPaymentMethod.setEnabled(true);
        spinAmountPaid.setEnabled(true);
        spinAmountPaid.setValue(selectedOrderAmount); // Auto-fill full price
        btnProcessPayment.setEnabled(true);
    }

    private void resetConsole() {
        selectedOrderId = -1;
        selectedOrderAmount = 0.0;
        lblSelectedOrder.setText("Please select a pending order...");
        lblSelectedOrder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblSelectedOrder.setForeground(Color.GRAY);
        cmbPaymentMethod.setEnabled(false);
        spinAmountPaid.setEnabled(false);
        spinAmountPaid.setValue(0.0);
        btnProcessPayment.setEnabled(false);
    }

    private void processCheckout() {
        if (selectedOrderId == -1) return;

        double amountPaid = (double) spinAmountPaid.getValue();
        if (amountPaid <= 0) {
            JOptionPane.showMessageDialog(this, "Amount paid must be greater than zero.", "Validation Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (amountPaid < selectedOrderAmount) {
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Amount paid ($" + amountPaid + ") is less than the total invoice value ($" + selectedOrderAmount + ").\nDo you want to proceed with this payment?", 
                    "Partial Payment Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        String paymentMethod = (String) cmbPaymentMethod.getSelectedItem();

        Connection conn = null;
        PreparedStatement stmtPay = null;
        PreparedStatement stmtOrder = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Insert Payment Record
            String insertPayment = "INSERT INTO payments (order_id, amount, payment_method, status) VALUES (?, ?, ?, 'Completed')";
            stmtPay = conn.prepareStatement(insertPayment);
            stmtPay.setInt(1, selectedOrderId);
            stmtPay.setDouble(2, amountPaid);
            stmtPay.setString(3, paymentMethod);
            stmtPay.executeUpdate();

            // 2. Update Order Status to Completed
            String updateOrder = "UPDATE orders SET status = 'Completed' WHERE id = ?";
            stmtOrder = conn.prepareStatement(updateOrder);
            stmtOrder.setInt(1, selectedOrderId);
            stmtOrder.executeUpdate();

            conn.commit(); // Commit Transaction

            JOptionPane.showMessageDialog(this, 
                    "Payment of $" + String.format("%.2f", amountPaid) + " processed successfully for Order #" + selectedOrderId + ".", 
                    "Checkout Successful", JOptionPane.INFORMATION_MESSAGE);

            // Reload data
            loadPendingOrders();
            loadPaymentHistory();

        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException r) { System.err.println("Rollback error: " + r.getMessage()); }
            }
            JOptionPane.showMessageDialog(this, "Failed to process payment: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (stmtPay != null) stmtPay.close();
                if (stmtOrder != null) stmtOrder.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}
