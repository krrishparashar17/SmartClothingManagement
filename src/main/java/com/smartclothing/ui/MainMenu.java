package com.smartclothing.ui;

import com.smartclothing.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainMenu extends JFrame {
    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Sub-panels (Views)
    private JPanel dashboardPanel;
    private ProductForm productForm;
    private CustomerForm customerForm;
    private OrderForm orderForm;
    private PaymentForm paymentForm;
    private SalesAnalyticsPanel analyticsPanel;

    // Sidebar navigation buttons
    private JButton btnDash, btnProd, btnCust, btnOrd, btnPay, btnAnal;
    private JButton activeButton = null;

    // Dashboard dynamic items
    private JLabel lblDashSales, lblDashStockAlerts, lblDashCusts;
    private DefaultTableModel lowStockModel;

    public MainMenu() {
        setTitle("Smart Clothing Sales & Inventory Management System");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Layout
        setLayout(new BorderLayout());

        // Initialize Card Layout and Panel
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Instantiate Modules
        productForm = new ProductForm();
        customerForm = new CustomerForm();
        orderForm = new OrderForm();
        paymentForm = new PaymentForm();
        analyticsPanel = new SalesAnalyticsPanel();

        // Build UI Components
        initSidebar();
        initDashboardView();

        // Add views to card layout
        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(productForm, "products");
        contentPanel.add(customerForm, "customers");
        contentPanel.add(orderForm, "orders");
        contentPanel.add(paymentForm, "payments");
        contentPanel.add(analyticsPanel, "analytics");

        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Select default page
        switchView("dashboard", btnDash);
        loadDashboardData();
    }

    private void initSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(240, getHeight()));
        sidebarPanel.setBackground(new Color(15, 23, 42)); // Deep Slate Navy
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // Brand Label
        JLabel brandLabel = new JLabel("SmartClothing");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subBrandLabel = new JLabel("Sales & Inventory");
        subBrandLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subBrandLabel.setForeground(new Color(148, 163, 184)); // Muted Grey
        subBrandLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebarPanel.add(brandLabel);
        sidebarPanel.add(subBrandLabel);
        sidebarPanel.add(Box.createVerticalStrut(40));

        // Navigation Buttons
        btnDash = createNavButton("Dashboard");
        btnProd = createNavButton("Products");
        btnCust = createNavButton("Customers");
        btnOrd = createNavButton("Orders");
        btnPay = createNavButton("Payments");
        btnAnal = createNavButton("Analytics");

        // Action Listeners
        btnDash.addActionListener(e -> {
            loadDashboardData();
            switchView("dashboard", btnDash);
        });
        btnProd.addActionListener(e -> {
            productForm.loadProductData();
            switchView("products", btnProd);
        });
        btnCust.addActionListener(e -> {
            customerForm.loadCustomerData();
            switchView("customers", btnCust);
        });
        btnOrd.addActionListener(e -> {
            orderForm.refreshNewOrderForm();
            orderForm.loadOrderHistory();
            switchView("orders", btnOrd);
        });
        btnPay.addActionListener(e -> {
            paymentForm.loadPendingOrders();
            paymentForm.loadPaymentHistory();
            switchView("payments", btnPay);
        });
        btnAnal.addActionListener(e -> {
            analyticsPanel.loadAnalyticsData();
            switchView("analytics", btnAnal);
        });

        sidebarPanel.add(btnDash);
        sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnProd);
        sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnCust);
        sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnOrd);
        sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnPay);
        sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnAnal);

        // Sidebar Footer
        sidebarPanel.add(Box.createVerticalGlue());
        
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        versionLabel.setForeground(new Color(100, 116, 139));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebarPanel.add(versionLabel);
    }

    private JButton createNavButton(String name) {
        JButton btn = new JButton(name) {
            @Override
            protected void paintComponent(Graphics g) {
                // If it is active, draw a custom highlight background and active bar
                if (activeButton == this) {
                    g.setColor(new Color(30, 41, 59)); // Slate
                    g.fillRect(0, 0, getWidth(), getHeight());
                    
                    g.setColor(new Color(99, 102, 241)); // Indigo Accent indicator
                    g.fillRect(0, 0, 5, getHeight());
                }
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btn.setMaximumSize(new Dimension(220, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (activeButton != btn) {
                    btn.setForeground(new Color(129, 140, 248)); // Light Indigo
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (activeButton != btn) {
                    btn.setForeground(Color.WHITE);
                }
            }
        });

        return btn;
    }

    private void switchView(String cardName, JButton triggerBtn) {
        if (activeButton != null) {
            activeButton.setForeground(Color.WHITE);
        }
        activeButton = triggerBtn;
        activeButton.setForeground(new Color(99, 102, 241)); // Indigo text for selected

        cardLayout.show(contentPanel, cardName);
        sidebarPanel.repaint();
    }

    // --- Dashboard View Creation ---
    private void initDashboardView() {
        dashboardPanel = new JPanel(new BorderLayout(15, 15));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top Greeting Panel
        JPanel greetPanel = new JPanel(new BorderLayout());
        greetPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Welcome Back, Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        JLabel subtitleLabel = new JLabel("Here's a snapshot of your sales and inventory operations today.");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);

        greetPanel.add(titleLabel, BorderLayout.NORTH);
        greetPanel.add(subtitleLabel, BorderLayout.SOUTH);
        dashboardPanel.add(greetPanel, BorderLayout.NORTH);

        // Center Panel (GridBag Layout for Cards and low stock list)
        JPanel centerGrid = new JPanel(new GridBagLayout());
        centerGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // KPI Panels Row
        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 20, 0));
        kpiRow.setOpaque(false);

        kpiRow.add(createDashboardKpiCard("TOTAL SALES", "$0.00", "sales"));
        kpiRow.add(createDashboardKpiCard("LOW STOCK ITEMS", "0 Alerts", "alerts"));
        kpiRow.add(createDashboardKpiCard("REGISTERED CUSTOMERS", "0 Profile(s)", "customers"));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.2;
        centerGrid.add(kpiRow, gbc);

        // Lower Row: Low stock panel & Quick actions
        JPanel alertPanel = new JPanel(new BorderLayout(10, 10));
        alertPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Critical Inventory Alerts (Low/Out of Stock)"));

        String[] alertCols = {"Product", "Size", "Current Stock", "Min Required"};
        lowStockModel = new DefaultTableModel(alertCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable alertTable = new JTable(lowStockModel);
        alertTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        alertTable.setRowHeight(25);
        alertTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane alertScroll = new JScrollPane(alertTable);
        alertPanel.add(alertScroll, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.65; gbc.weighty = 0.8;
        centerGrid.add(alertPanel, gbc);

        // Quick Shortcuts Panel
        JPanel shortcutsPanel = new JPanel(new GridLayout(4, 1, 0, 15));
        shortcutsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Quick Controls"));
        shortcutsPanel.setPreferredSize(new Dimension(280, 0));

        JButton btnQuickOrder = new JButton("Create New Order");
        btnQuickOrder.setBackground(new Color(99, 102, 241));
        btnQuickOrder.setForeground(Color.WHITE);
        btnQuickOrder.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnQuickOrder.addActionListener(e -> {
            orderForm.refreshNewOrderForm();
            orderForm.loadOrderHistory();
            switchView("orders", btnOrd);
        });

        JButton btnQuickProduct = new JButton("Add Clothing Product");
        btnQuickProduct.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnQuickProduct.addActionListener(e -> {
            productForm.loadProductData();
            switchView("products", btnProd);
        });

        JButton btnQuickPay = new JButton("Process Outstanding Bills");
        btnQuickPay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnQuickPay.addActionListener(e -> {
            paymentForm.loadPendingOrders();
            paymentForm.loadPaymentHistory();
            switchView("payments", btnPay);
        });

        JButton btnQuickAnalytics = new JButton("View Trend Analysis");
        btnQuickAnalytics.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnQuickAnalytics.addActionListener(e -> {
            analyticsPanel.loadAnalyticsData();
            switchView("analytics", btnAnal);
        });

        shortcutsPanel.add(btnQuickOrder);
        shortcutsPanel.add(btnQuickProduct);
        shortcutsPanel.add(btnQuickPay);
        shortcutsPanel.add(btnQuickAnalytics);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.35; gbc.weighty = 0.8;
        centerGrid.add(shortcutsPanel, gbc);

        dashboardPanel.add(centerGrid, BorderLayout.CENTER);
    }

    private JPanel createDashboardKpiCard(String title, String val, String key) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220, 45), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setBackground(UIManager.getColor("Panel.background"));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(Color.GRAY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(val);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        if ("sales".equals(key)) {
            lblDashSales = valLbl;
            valLbl.setForeground(new Color(16, 185, 129));
        } else if ("alerts".equals(key)) {
            lblDashStockAlerts = valLbl;
            valLbl.setForeground(new Color(244, 63, 94));
        } else if ("customers".equals(key)) {
            lblDashCusts = valLbl;
            valLbl.setForeground(new Color(99, 102, 241));
        }

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(valLbl);

        return card;
    }

    private void loadDashboardData() {
        lowStockModel.setRowCount(0);

        // Fetch metrics & populate alerts
        try (Connection conn = DBConnection.getConnection()) {
            
            // 1. Total Completed Sales Revenue
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'Completed'");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lblDashSales.setText("$" + String.format("%.2f", rs.getDouble(1)));
                }
            }

            // 2. Count Customers
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM customers");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lblDashCusts.setText(rs.getInt(1) + " Profile(s)");
                }
            }

            // 3. Count Low stock products & Populate list
            int alertCount = 0;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT name, size, stock_quantity, min_stock_level FROM products WHERE stock_quantity <= min_stock_level ORDER BY stock_quantity ASC");
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    alertCount++;
                    lowStockModel.addRow(new Object[]{
                            rs.getString("name"),
                            rs.getString("size"),
                            rs.getInt("stock_quantity"),
                            rs.getInt("min_stock_level")
                    });
                }
            }
            lblDashStockAlerts.setText(alertCount + " Alert(s)");

        } catch (SQLException e) {
            System.err.println("Error loading dashboard indicators: " + e.getMessage());
        }
    }
}
