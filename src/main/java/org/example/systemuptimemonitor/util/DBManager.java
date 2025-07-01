//package org.example.systemuptimemonitor.util;
//
//import javax.naming.Context;
//import javax.naming.InitialContext;
//import javax.naming.NamingException;
//import javax.sql.DataSource;
//
//public class DBManager {
//    private final static DataSource ds;
//
//    static {
//        try {
//            Context initialContext = new InitialContext();
//            Context envContext = (Context) initialContext.lookup("java:/comp/env");
//            ds = (DataSource) envContext.lookup("jdbc/SystemUptimeDB");
//        } catch (NamingException e) {
//            throw new RuntimeException(e);
//        }
//
//    }
//
//    public static DataSource getDataSource() {
//        return ds;
//    }
//}
