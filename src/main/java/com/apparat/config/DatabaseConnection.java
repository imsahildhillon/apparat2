
package com.apparat.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // The database address, user and password can be changed with Java
    // system properties, for example -Dapparat.db.url=jdbc:mysql://host/db
    // The tests use this to run against apparat2_test instead of apparat2.
    private static final String URL = System.getProperty(
            "apparat.db.url",
            "jdbc:mysql://127.0.0.1:3308/apparat2"
    );

    private static final String USER =
            System.getProperty("apparat.db.user", "root");

    private static final String PASSWORD =
            System.getProperty("apparat.db.password", "");

    public static String getUrl() {
        return URL;
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver could not be loaded.",
                    e
            );
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
