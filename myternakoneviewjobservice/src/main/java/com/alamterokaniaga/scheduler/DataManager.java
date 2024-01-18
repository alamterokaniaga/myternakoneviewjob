package com.alamterokaniaga.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    public static List<DataRow> fetchData(Connection connection) {
        List<DataRow> dataRows = new ArrayList<>();

        try {
            // Example query
            String sqlQuery = "EXECUTE dbo.[spGetAdsListingPushNotificationDBValue]";

            // Create a statement
            Statement statement = connection.createStatement();

            // Execute the query
            ResultSet resultSet = statement.executeQuery(sqlQuery);

            // Process the results
            while (resultSet.next()) {
                // Retrieve data from the result set
                int RowID = resultSet.getInt("RowID");
                String ProfileFCMToken = resultSet.getString("ProfileFCMToken");
                String ListingTypeCategory = resultSet.getString("ListingTypeCategory");
                String ListingUserName = resultSet.getString("ListingUserName");
                String ListingContent = resultSet.getString("ListingContent");
                int ThreadID = resultSet.getInt("ThreadID");
                String PusherID = resultSet.getString("PusherID");

                // Create DataRow object and add it to the list
                DataRow dataRow = new DataRow(RowID, ProfileFCMToken, ListingTypeCategory, ListingUserName,
                        ListingContent,
                        ThreadID, PusherID);
                dataRows.add(dataRow);
            }

            // Close resources
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error executing SQL query: " + e.getMessage());
        }

        return dataRows;
    }

    public static void addNotificationRecord(Connection connection, String PushActivityReferenceID,
            String PushActivityStatus, String PushActivityDescription)
            throws SQLException {
        String sql = "INSERT INTO tblPushActivity (PushActivityReferenceID, PushActivityStatus, PushActivityDescription, PushActivityCreatedDate, PushActivityModifiedDate) VALUES (?, ?, ?, GETDATE(), GETDATE())";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PushActivityReferenceID);
            statement.setString(2, PushActivityStatus);
            statement.setString(3, PushActivityDescription);

            // Execute the insert statement
            statement.executeUpdate();

            System.out.println("Successfully added record into database");
        }
    }

    static class DataRow {
        int RowID;
        String ProfileFCMToken;
        String ListingTypeCategory;
        String ListingUserName;
        String ListingContent;
        int ThreadID;
        String PusherID;

        public DataRow(int RowID, String ProfileFCMToken, String ListingTypeCategory, String ListingUserName,
                String ListingContent, int ThreadID, String PusherID) {
            this.RowID = RowID;
            this.ProfileFCMToken = ProfileFCMToken;
            this.ListingTypeCategory = ListingTypeCategory;
            this.ListingUserName = ListingUserName;
            this.ListingContent = ListingContent;
            this.ThreadID = ThreadID;
            this.PusherID = PusherID;
        }
    }
}
