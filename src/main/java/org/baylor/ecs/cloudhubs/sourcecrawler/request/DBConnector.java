package org.baylor.ecs.cloudhubs.sourcecrawler.request;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Basic db connection, will use later to store request info
//public class DBConnector {
//
//    public Connection startConn() throws SQLException {
//        // auto close connection
//        try (Connection conn = DriverManager.getConnection(
//                "jdbc:postgresql://127.0.0.1:5432/test", "postgres", "password")) {
//
//            if (conn != null) {
//                System.out.println("Connected to the database!");
//            } else {
//                System.out.println("Failed to make connection!");
//            }
//
//        } catch (SQLException e) {
//            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
