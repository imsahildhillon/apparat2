package com.apparat.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResourceConnectionScratch {

    public static void main(String[] args) {
        String sql =
                "SELECT id, name, location, status FROM resources";

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            System.out.println("Connected to apparat2!");
            System.out.println("Resources:");

            while (resultSet.next()) {

                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String location = resultSet.getString("location");
                String status = resultSet.getString("status");

                System.out.println(
                        id + " | " + name + " | "
                                + location + " | " + status
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}