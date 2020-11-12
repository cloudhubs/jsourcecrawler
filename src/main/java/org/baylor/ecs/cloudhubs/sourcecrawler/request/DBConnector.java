//package org.baylor.ecs.cloudhubs.sourcecrawler.request;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.util.Properties;
//
//// Basic db connection, will use later to store request info
//public class DBConnector {
//
//    public static Connection startConn() throws SQLException {
//
//        Connection conn = null;
//        // auto close connection
//        try{
//            conn = DriverManager.getConnection(
//                    "jdbc:postgresql://127.0.0.1:5432/test",
//                    "postgres",
//                    "password");
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
//
//        return conn;
//    }
//
//    public static void main(String[] args) throws SQLException {
//
//    }
//}
