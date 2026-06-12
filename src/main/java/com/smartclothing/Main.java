package com.smartclothing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.smartclothing.db.DBConnection;
import com.smartclothing.ui.MainMenu;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Configure FlatLaf Dark Theme for professional modern styling
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            // Customize table selection colors slightly
            UIManager.put("Table.selectionBackground", new Color(99, 102, 241, 100));
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf theme: " + ex.getMessage());
        }

        // Check database connection on startup
        boolean connected = false;
        while (!connected) {
            if (DBConnection.testConnection()) {
                connected = true;
            } else {
                // Connection failed. Prompt user with a modern option panel.
                String message = "Unable to connect to PostgreSQL database at:\n" +
                                 DBConnection.getUrl() + "\n" +
                                 "User: " + DBConnection.getUsername() + "\n\n" +
                                 "Please ensure:\n" +
                                 "1. PostgreSQL service is running.\n" +
                                 "2. Database 'smart_clothing_db' exists.\n" +
                                 "3. Credentials in src/main/resources/db.properties are correct.";
                
                String[] options = {"Retry Connection", "Run in Demo/Offline Mode", "Exit"};
                int choice = JOptionPane.showOptionDialog(
                        null,
                        message,
                        "Database Connection Failure",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                if (choice == JOptionPane.YES_OPTION) {
                    // Loop will retry
                    System.out.println("Retrying database connection...");
                } else if (choice == JOptionPane.NO_OPTION) {
                    // Offline Demo mode
                    System.out.println("Starting application in Offline Demo Mode...");
                    break;
                } else {
                    // Cancel or close window - exit app
                    System.out.println("Exiting application.");
                    System.exit(0);
                }
            }
        }

        // Launch Application Main GUI on the Event Dispatch Thread
        final boolean dbAvailable = connected;
        SwingUtilities.invokeLater(() -> {
            try {
                MainMenu menu = new MainMenu();
                if (!dbAvailable) {
                    // Indicate demo mode on window title
                    menu.setTitle(menu.getTitle() + " [OFFLINE DEMO MODE]");
                    JOptionPane.showMessageDialog(menu, 
                            "Application started in Demo Mode.\nDatabase actions (Create, Read, Update, Delete) may fail or display errors without PostgreSQL server connection.", 
                            "Offline Notice", JOptionPane.INFORMATION_MESSAGE);
                }
                menu.setVisible(true);
            } catch (Exception e) {
                System.err.println("Fatal error launching UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
