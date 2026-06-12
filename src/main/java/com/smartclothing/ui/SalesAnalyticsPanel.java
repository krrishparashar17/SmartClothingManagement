package com.smartclothing.ui;

import com.smartclothing.db.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SalesAnalyticsPanel extends JPanel {
    private JComboBox<String> cmbTimeframe;
    private JButton btnRefresh;
    private JLabel lblTotalRevenue, lblTotalOrders, lblAvgOrderValue;
    private DefaultTableModel topProductsModel;
    private JTable topProductsTable;
    private SalesGraphPanel graphPanel;

    public SalesAnalyticsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        loadAnalyticsData();
    }

    private void initComponents() {
        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Sales & Inventory Analytics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);

        cmbTimeframe = new JComboBox<>(new String[]{"Last 7 Days", "Last 30 Days"});
        cmbTimeframe.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbTimeframe.addActionListener(e -> loadAnalyticsData());

        btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.addActionListener(e -> loadAnalyticsData());

        filterPanel.add(new JLabel("Timeframe:"));
        filterPanel.add(cmbTimeframe);
        filterPanel.add(btnRefresh);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Dashboard Content (Center) ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // KPI Summary Panel (Top row of content)
        JPanel kpiPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        kpiPanel.setOpaque(false);

        kpiPanel.add(createKpiCard("TOTAL REVENUE", "$0.00", new Color(99, 102, 241))); // Indigo
        kpiPanel.add(createKpiCard("COMPLETED ORDERS", "0", new Color(16, 185, 129)));   // Emerald
        kpiPanel.add(createKpiCard("AVERAGE ORDER VALUE", "$0.00", new Color(245, 158, 11))); // Amber

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.15;
        contentPanel.add(kpiPanel, gbc);

        // Sales Graph Card
        JPanel graphCard = new JPanel(new BorderLayout(10, 10));
        graphCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Revenue Trend"));
        graphPanel = new SalesGraphPanel();
        graphCard.add(graphPanel, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.65; gbc.weighty = 0.85;
        contentPanel.add(graphCard, gbc);

        // Top Selling Products Card
        JPanel topProductsCard = new JPanel(new BorderLayout(10, 10));
        topProductsCard.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Top Selling Products"));
        
        String[] cols = {"Product Name", "Quantity Sold"};
        topProductsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        topProductsTable = new JTable(topProductsModel);
        topProductsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topProductsTable.setRowHeight(26);
        topProductsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JScrollPane prodScroll = new JScrollPane(topProductsTable);
        topProductsCard.add(prodScroll, BorderLayout.CENTER);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.35; gbc.weighty = 0.85;
        contentPanel.add(topProductsCard, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createKpiCard(String title, String defaultValue, Color topBorderColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(topBorderColor);
                g.fillRect(0, 0, getWidth(), 5); // Colored top border
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220, 30), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        card.setBackground(UIManager.getColor("Panel.background"));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLabel = new JLabel(defaultValue);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (title.contains("REVENUE")) {
            lblTotalRevenue = valLabel;
        } else if (title.contains("ORDERS")) {
            lblTotalOrders = valLabel;
        } else if (title.contains("VALUE")) {
            lblAvgOrderValue = valLabel;
        }

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valLabel);

        return card;
    }

    public void loadAnalyticsData() {
        String timeframe = (String) cmbTimeframe.getSelectedItem();
        int days = 7;
        if ("Last 30 Days".equals(timeframe)) {
            days = 30;
        }

        loadKpis(days);
        loadTopProducts(days);
        loadSalesTrend(days);
    }

    private void loadKpis(int days) {
        String query = "SELECT " +
                       "  COALESCE(SUM(total_amount), 0) AS total_revenue, " +
                       "  COUNT(*) AS total_orders, " +
                       "  COALESCE(AVG(total_amount), 0) AS avg_value " +
                       "FROM orders " +
                       "WHERE status = 'Completed' " +
                       "  AND order_date >= CURRENT_DATE - CAST(? AS INTERVAL)";

        // Postgres handles INTERVAL casting easily using make_interval or concatenating string
        String postgresQuery = "SELECT " +
                               "  COALESCE(SUM(total_amount), 0) AS total_revenue, " +
                               "  COUNT(*) AS total_orders, " +
                               "  COALESCE(AVG(total_amount), 0) AS avg_value " +
                               "FROM orders " +
                               "WHERE status = 'Completed' " +
                               "  AND order_date >= NOW() - (? * INTERVAL '1 day')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(postgresQuery)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double revenue = rs.getDouble("total_revenue");
                    int orders = rs.getInt("total_orders");
                    double avgVal = rs.getDouble("avg_value");

                    lblTotalRevenue.setText("$" + String.format("%.2f", revenue));
                    lblTotalOrders.setText(String.valueOf(orders));
                    lblAvgOrderValue.setText("$" + String.format("%.2f", avgVal));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading KPIs: " + e.getMessage());
        }
    }

    private void loadTopProducts(int days) {
        topProductsModel.setRowCount(0);

        String query = "SELECT p.name AS prod_name, SUM(oi.quantity) AS qty_sold " +
                       "FROM order_items oi " +
                       "JOIN products p ON oi.product_id = p.id " +
                       "JOIN orders o ON oi.order_id = o.id " +
                       "WHERE o.status = 'Completed' " +
                       "  AND o.order_date >= NOW() - (? * INTERVAL '1 day') " +
                       "GROUP BY p.name " +
                       "ORDER BY qty_sold DESC " +
                       "LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topProductsModel.addRow(new Object[]{rs.getString("prod_name"), rs.getInt("qty_sold")});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading top products: " + e.getMessage());
        }
    }

    private void loadSalesTrend(int days) {
        List<Double> sales = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        String query = "SELECT DATE_TRUNC('day', order_date) AS sale_date, SUM(total_amount) AS daily_total " +
                       "FROM orders " +
                       "WHERE status = 'Completed' " +
                       "  AND order_date >= CURRENT_DATE - (? - 1) " +
                       "GROUP BY sale_date " +
                       "ORDER BY sale_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, days);
            try (ResultSet rs = stmt.executeQuery()) {
                SimpleDateFormat sdf = new SimpleDateFormat(days <= 7 ? "EEE" : "dd MMM");
                while (rs.next()) {
                    Date date = rs.getDate("sale_date");
                    double total = rs.getDouble("daily_total");
                    sales.add(total);
                    labels.add(sdf.format(date));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading sales trend: " + e.getMessage());
        }

        // Fallback placeholder if no completed orders exist yet
        if (sales.isEmpty()) {
            for (int i = days - 1; i >= 0; i--) {
                sales.add(0.0);
                labels.add("-");
            }
        }

        graphPanel.setData(sales, labels);
    }

    // --- Custom Drawing Graph Component ---
    private static class SalesGraphPanel extends JPanel {
        private List<Double> data = new ArrayList<>();
        private List<String> labels = new ArrayList<>();

        public SalesGraphPanel() {
            setOpaque(false);
            setBackground(Color.WHITE);
        }

        public void setData(List<Double> data, List<String> labels) {
            this.data = data;
            this.labels = labels;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            int padding = 40;
            int labelPadding = 25;

            // Find max value in data
            double maxVal = Collections.max(data);
            if (maxVal == 0) maxVal = 100.0; // scale fallback

            // Draw gridlines
            g2d.setColor(new Color(220, 220, 220, 40));
            int numYDivisions = 5;
            for (int i = 0; i <= numYDivisions; i++) {
                int y = height - ((i * (height - padding * 2)) / numYDivisions + padding);
                g2d.drawLine(padding + labelPadding, y, width - padding, y);
                
                // Draw Y labels
                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                double val = (maxVal * i) / numYDivisions;
                g2d.drawString(String.format("$%.0f", val), padding - 10, y + 4);
                g2d.setColor(new Color(220, 220, 220, 40));
            }

            // Calculate graph coordinates
            int size = data.size();
            int[] xPoints = new int[size];
            int[] yPoints = new int[size];
            double xInterval = (double) (width - padding * 2 - labelPadding) / (size - 1 == 0 ? 1 : size - 1);

            for (int i = 0; i < size; i++) {
                xPoints[i] = (int) (i * xInterval) + padding + labelPadding;
                double ratio = data.get(i) / maxVal;
                yPoints[i] = (int) ((1 - ratio) * (height - padding * 2)) + padding;
            }

            // Draw filled gradient area under the line
            GeneralPath area = new GeneralPath();
            area.moveTo(xPoints[0], height - padding);
            for (int i = 0; i < size; i++) {
                area.lineTo(xPoints[i], yPoints[i]);
            }
            area.lineTo(xPoints[size - 1], height - padding);
            area.closePath();

            // Gradient: primary theme color to transparent
            Color themeColor = new Color(99, 102, 241, 100);
            GradientPaint gp = new GradientPaint(
                    0, padding, themeColor,
                    0, height - padding, new Color(99, 102, 241, 0)
            );
            g2d.setPaint(gp);
            g2d.fill(area);

            // Draw line
            g2d.setColor(new Color(99, 102, 241));
            g2d.setStroke(new BasicStroke(3f));
            for (int i = 0; i < size - 1; i++) {
                g2d.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            // Draw node dots & X labels
            g2d.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < size; i++) {
                // Circle point
                g2d.setColor(Color.WHITE);
                g2d.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
                g2d.setColor(new Color(99, 102, 241));
                g2d.drawOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);

                // Label
                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                
                // Draw labels (only skip to fit if last 30 days)
                if (size <= 7 || i % 5 == 0 || i == size - 1) {
                    String label = labels.get(i);
                    int labelWidth = g2d.getFontMetrics().stringWidth(label);
                    g2d.drawString(label, xPoints[i] - labelWidth / 2, height - padding + 18);
                }
            }
        }
    }
}
