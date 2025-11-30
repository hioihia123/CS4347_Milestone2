package com.checkmates.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/checkmates";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // MySQL installed without password - leave empty or set one with mysql_secure_installation

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
