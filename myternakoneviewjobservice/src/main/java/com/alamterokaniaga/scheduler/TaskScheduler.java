package com.alamterokaniaga.scheduler;

import com.alamterokaniaga.config.ConfigLoader;
import com.alamterokaniaga.database.DatabaseConnector;
import com.alamterokaniaga.fcm.FCMSender;
import com.alamterokaniaga.firebase.FirebaseInitializer;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private static boolean isTaskARunning = false; // Flag to track Task A activity

    public static void main(String[] args) {
        // Load configuration from properties file
        Properties properties = ConfigLoader.loadConfiguration("appsettings.properties");

        // Create a ScheduledExecutorService with a single thread
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Use CountDownLatch to synchronize the start of Task B
        CountDownLatch taskACompleted = new CountDownLatch(1);

        // Schedule your task to run every 10 minutes
        scheduler.scheduleAtFixedRate(() -> {
            // Check if Task A is running
            if (!isTaskARunning) {
                Connection connection = null; // Declare outside try block to be visible in finally block
                try {

                    // Initialize Firebase
                    FirebaseInitializer.initialize();
                    // Set your database connection parameters
                    // Use the loaded properties in your code
                    String jdbcUrl = properties.getProperty("jdbc.url");
                    String username = properties.getProperty("jdbc.username");
                    String password = properties.getProperty("jdbc.password");

                    // Get a database connection
                    connection = DatabaseConnector.getConnection(jdbcUrl, username, password);
                    System.out.println("Connected to the database!");

                    // Call the method to fetch data
                    List<DataManager.DataRow> dataRows = DataManager.fetchData(connection);

                    // Process the data and send FCM notifications
                    for (DataManager.DataRow dataRow : dataRows) {
                        // Process the data as needed
                        System.out.println("Data: " + dataRow.ListingTypeCategory);
                        try {
                            // Check if ProfileFCMToken, ListingTypeCategory, and ListingContent are not
                            // empty or null
                            if (!isEmptyOrNull(dataRow.ProfileFCMToken) && !isEmptyOrNull(dataRow.ListingTypeCategory)
                                    && !isEmptyOrNull(dataRow.ListingContent)) {
                                // Send push notification
                                FCMSender.sendPushNotification(dataRow.ProfileFCMToken, dataRow.ListingTypeCategory,
                                        dataRow.ListingContent, "ADSLISTING", String.valueOf(dataRow.ThreadID), // Convert
                                                                                                                // int
                                                                                                                // to
                                        // String implicitly
                                        dataRow.ListingUserName);
                                // If push notification isclear successful, add a record to the database
                                DataManager.addNotificationRecord(connection, dataRow.PusherID, "COMPLETE",
                                        "");
                            } else {
                                System.out.println(
                                        "Skipping push notification due to empty or null values in column2, column3, or column5.");
                            }
                        } catch (Exception e) {
                            // If an error occurs during push notification, add a record with error details
                            // to the database
                            DataManager.addNotificationRecord(connection, dataRow.PusherID, "ERROR", e.getMessage());
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error connecting to the database: " + e.getMessage());
                } finally {
                    // Close the connection in the finally block to ensure it's done even if an
                    // exception occurs
                    if (connection != null) {
                        try {
                            // Close the connection
                            connection.close();
                        } catch (SQLException e) {
                            System.err.println("Error closing the database connection: " + e.getMessage());
                        }
                    }
                    isTaskARunning = false; // Reset the flag to indicate Task A has completed
                }
            } else {
                System.out.println("Task A is currently running. Waiting for it to complete.");
            }
            // Notify that Task A has completed
            taskACompleted.countDown();
        }, 0, 10, TimeUnit.MINUTES);

        // Start a separate thread to handle Task B
        new Thread(() -> {
            try {
                // Wait for Task A to complete at least once
                taskACompleted.await();

                // Schedule Task B to run one hour after the service is started
                scheduler.schedule(() -> {
                    try {
                        // Wait for Task A to complete at least once
                        taskACompleted.await();

                        // Schedule Task B to run hourly
                        scheduler.scheduleAtFixedRate(() -> {
                            try {
                                // Check Task A activity
                                if (!isTaskARunning) {
                                    System.out.println("No activity from Task A. Proceeding with Task B.");

                                    // Restart Java Application Service
                                    restartJavaApplicationService(properties.getProperty("service.name"),
                                            properties.getProperty("prunsrvPath"),
                                            properties.getProperty("jarDirectory"));

                                    // Garbage Memory Cleanup
                                    performGarbageMemoryCleanup();

                                } else {
                                    System.out.println("Task A is currently running. Skipping Task B.");
                                }
                            } catch (Exception e) {
                                // Handle any exceptions
                                e.printStackTrace();
                            }
                        }, 0, 1, TimeUnit.HOURS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, 1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Add a shutdown hook to shut down the scheduler gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    // Add a utility method to check if a string is empty or null
    private static boolean isEmptyOrNull(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void restartJavaApplicationService(String serviceName, String prunsrvPath, String jarDirectory) {
        System.out.println("Restarting Java Application Service...");
        try {
            // Stop the service
            String stopCommand = prunsrvPath + " //US//" + serviceName + " --Stop";
            Process stopProcess = Runtime.getRuntime().exec(stopCommand, null, new File(jarDirectory));
            stopProcess.waitFor(); // Wait for the process to finish
			System.out.println("Service Stopped...");										 

            // Start the service
            String startCommand = prunsrvPath + " //US//" + serviceName + " --Start";
            Process startProcess = Runtime.getRuntime().exec(startCommand, null, new File(jarDirectory));
            startProcess.waitFor(); // Wait for the process to finish
			System.out.println("Service Started...");										 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void performGarbageMemoryCleanup() {
        System.out.println(
                "Performing garbage memory cleanup typically relies on the Java garbage collector. In your Java application, you don't need to explicitly manage garbage memory cleanup unless you have specific requirements. The Java Virtual Machine (JVM) handles this automatically.\r\n"
                        + //
                        "\r\n" + //
                        "If you have specific memory management requirements, consider using tools like Java VisualVM or jcmd to monitor and manage memory.\r\n"
                        + //
                        "\r\n" + //
                        "Your current implementation of performGarbageMemoryCleanup() can be simplified or omitted unless you have specific memory-related tasks.");
    }
}

