package com.smartclothing.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static String url = "jdbc:postgresql://localhost:5432/smart_clothing_db";
    private static String username = "postgres";
    private static String password = "admin";

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                url = prop.getProperty("db.url", url);
                username = prop.getProperty("db.username", username);
                password = prop.getProperty("db.password", password);
            } else {
                System.out.println("Warning: db.properties not found. Using default connection settings.");
            }
        } catch (Exception ex) {
            System.err.println("Error loading db.properties: " + ex.getMessage());
        }
    }

    /**
     * Gets a new connection to the database.
     * The caller is responsible for closing the connection.
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found in classpath.", e);
        }
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Tests if the database connection can be established.
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }
}
