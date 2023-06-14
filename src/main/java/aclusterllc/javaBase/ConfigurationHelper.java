package aclusterllc.javaBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ConfigurationHelper {
    public static final Properties configIni = new Properties();
    static Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);
    private static HikariDataSource hikariDataSource;
    public static final JSONObject dbBasicInfo = new JSONObject();
    public static final JSONObject countersCurrentValue = new JSONObject();
    public static final JSONObject motorsCurrentSpeed = new JSONObject();
    public static final Map<Integer, Integer> apeClientConnectionStatus  = new HashMap<>();
    public static final JSONObject systemConstants = new JSONObject();





    public static void loadIniConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
        }
        catch (IOException e)
        {
            logger.info("File Config Read failed"+e);
            System.exit(0);
        }
        setSystemConstants();

    }
    public static void setSystemConstants(){
        JSONObject APE_MESSAGE_ID_NAME  = new JSONObject();

        APE_MESSAGE_ID_NAME.put("1", "System State Message");
        APE_MESSAGE_ID_NAME.put("2", "Inputs Message");
        APE_MESSAGE_ID_NAME.put("3", "Input Change Message");
        APE_MESSAGE_ID_NAME.put("4", "Errors Message");
        APE_MESSAGE_ID_NAME.put("5", "Jams Message");
        APE_MESSAGE_ID_NAME.put("6", "ConfirmationPEBlocked Message");
        APE_MESSAGE_ID_NAME.put("7", "ConfirmationPEBlocked change Message");
        APE_MESSAGE_ID_NAME.put("8", "BinPartiallyFull Message");
        APE_MESSAGE_ID_NAME.put("9", "BinPartiallyFull change Message");
        APE_MESSAGE_ID_NAME.put("10", "BinFull Message");
        APE_MESSAGE_ID_NAME.put("11", "BinFull change Message");
        APE_MESSAGE_ID_NAME.put("12", "BinDisabled Message");
        APE_MESSAGE_ID_NAME.put("13", "BinDisabled change Message");
        APE_MESSAGE_ID_NAME.put("14", "Devices Connected Message");
        APE_MESSAGE_ID_NAME.put("15", "Device Connected Change Message");
        APE_MESSAGE_ID_NAME.put("16", "Sync Response");
        APE_MESSAGE_ID_NAME.put("17", "TrayMissing Message");
        APE_MESSAGE_ID_NAME.put("18", "TrayMissing change Message");
        APE_MESSAGE_ID_NAME.put("20", "Dimension Message");
        APE_MESSAGE_ID_NAME.put("21", "Barcode Result");
        APE_MESSAGE_ID_NAME.put("22", "ConfirmDestination Message");
        APE_MESSAGE_ID_NAME.put("30", "Ping Response");
        APE_MESSAGE_ID_NAME.put("40", "BinMode Message");
        APE_MESSAGE_ID_NAME.put("41", "BinModechange Message");
        APE_MESSAGE_ID_NAME.put("42", "ConveyorState Message");
        APE_MESSAGE_ID_NAME.put("43", "ConveyorStateChange Message");
        APE_MESSAGE_ID_NAME.put("44", "SensorHit Message");
        APE_MESSAGE_ID_NAME.put("45", "Event Message");
        APE_MESSAGE_ID_NAME.put("46", "InductLineState Message");
        APE_MESSAGE_ID_NAME.put("47", "InductLineStateChange Message");
        APE_MESSAGE_ID_NAME.put("48", "PieceInducted Message");
        APE_MESSAGE_ID_NAME.put("49", "Motor Speed Message");
        APE_MESSAGE_ID_NAME.put("50", "EStop Message");
        APE_MESSAGE_ID_NAME.put("51", "Machine Stopped Message");
        APE_MESSAGE_ID_NAME.put("52", "Belt Status Message");
        APE_MESSAGE_ID_NAME.put("53", "Outputs Message");
        APE_MESSAGE_ID_NAME.put("54", "Param Value Message");
        APE_MESSAGE_ID_NAME.put("55", "RequestParams Message");
        APE_MESSAGE_ID_NAME.put("56", "Counter Message");
        APE_MESSAGE_ID_NAME.put("101", "Request Inputs State Message");
        APE_MESSAGE_ID_NAME.put("102", "Request Errors Message");
        APE_MESSAGE_ID_NAME.put("103", "Request Jams Message");
        APE_MESSAGE_ID_NAME.put("105", "Request ConfirmationPEBlocked Message");
        APE_MESSAGE_ID_NAME.put("106", "Request BinPartiallyFull Message");
        APE_MESSAGE_ID_NAME.put("107", "Request BinFull Message");
        APE_MESSAGE_ID_NAME.put("108", "Request BinDisabled Message");
        APE_MESSAGE_ID_NAME.put("109", "Request DevicesConnected State Message");
        APE_MESSAGE_ID_NAME.put("110", "Request BinMode Message");
        APE_MESSAGE_ID_NAME.put("111", "SetBinMode Message");
        APE_MESSAGE_ID_NAME.put("112", "Request ConveyorState Message");
        APE_MESSAGE_ID_NAME.put("113", "Request TrayMissing Message");
        APE_MESSAGE_ID_NAME.put("114", "Request InductLineState Message");
        APE_MESSAGE_ID_NAME.put("115", "Set Param Value Message");
        APE_MESSAGE_ID_NAME.put("116", "Sync Request");
        APE_MESSAGE_ID_NAME.put("120", "SetMode Message");
        APE_MESSAGE_ID_NAME.put("123", "Device Command Message");
        APE_MESSAGE_ID_NAME.put("124", "SortMailpiece Message");
        APE_MESSAGE_ID_NAME.put("125", "Authorization To Start Message");
        APE_MESSAGE_ID_NAME.put("130", "Ping Request");
        systemConstants.put("APE_MESSAGE_ID_NAME",APE_MESSAGE_ID_NAME);

        JSONObject SYSTEM_STATES  = new JSONObject();
        SYSTEM_STATES.put("0", "Not Ready");
        SYSTEM_STATES.put("1", "Ready");
        SYSTEM_STATES.put("2", "Starting");
        SYSTEM_STATES.put("3", "Running");
        SYSTEM_STATES.put("4", "Stopping");
        systemConstants.put("SYSTEM_STATES",SYSTEM_STATES);

        JSONObject SYSTEM_MODES  = new JSONObject();
        SYSTEM_MODES.put("0", "Auto");
        SYSTEM_MODES.put("1", "Manual");
        systemConstants.put("SYSTEM_MODES",SYSTEM_MODES);
    }
    public static void loadDatabaseConfig(){
        createDatabaseConnection();
        try {
            Connection connection=getConnection();
            Statement stmt = connection.createStatement();
            String query = "SELECT * FROM alarms";
            dbBasicInfo.put("alarms",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "alarm_id", "alarm_type"}));

            query = "SELECT * FROM bins";
            dbBasicInfo.put("bins",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "bin_id"}));

            query = "SELECT * FROM boards";
            dbBasicInfo.put("boards",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "board_id"}));

            query = "SELECT * FROM board_ios";
            dbBasicInfo.put("board_ios",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "id"}));

            query = "SELECT * FROM conveyors";
            dbBasicInfo.put("conveyors",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "conveyor_id"}));

            query = "SELECT * FROM devices";
            dbBasicInfo.put("devices",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "device_id"}));
            query = "SELECT * FROM events";
            dbBasicInfo.put("events",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "event_id"}));
            query = "SELECT * FROM inducts";
            dbBasicInfo.put("inducts",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "induct_id"}));

            query = "SELECT * FROM inputs";
            dbBasicInfo.put("inputs",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "input_id"}));

            query = "SELECT * FROM machines";
            JSONObject machines=DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id"});
            dbBasicInfo.put("machines",machines);
            for (String machineId : machines.keySet()) {
                for(int i=0;i<32;i++){
                    countersCurrentValue.put(machineId+"_"+(i+1),0);
                }
            }

            query = "SELECT * FROM motors";
            JSONObject motors=DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "motor_id"});
            dbBasicInfo.put("motors",motors);
            for (String motorKey : motors.keySet()) {
                motorsCurrentSpeed.put(motorKey,0);
            }

            query = "SELECT * FROM parameters";
            dbBasicInfo.put("parameters",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "param_id"}));

            query = "SELECT * FROM scs";
            dbBasicInfo.put("scs",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "value"}));

            query = "SELECT * FROM sensors";
            dbBasicInfo.put("sensors",DatabaseHelper.getSelectQueryResults(connection,query,new String[] { "machine_id", "sensor_id"}));
            query = "INSERT IGNORE INTO statistics (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics);" +
                    "INSERT IGNORE INTO statistics_minutely (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_minutely);" +
                    "INSERT IGNORE INTO statistics_hourly (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_hourly);" +
                    "INSERT IGNORE INTO statistics_counter (machine_id) SELECT DISTINCT machine_id FROM machines WHERE NOT EXISTS (SELECT * FROM statistics_counter);" +
                    "INSERT IGNORE INTO statistics_bins (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins);" +
                    "INSERT IGNORE INTO statistics_bins_counter (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins_counter);" +
                    "INSERT IGNORE INTO statistics_bins_hourly (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE NOT EXISTS (SELECT * FROM statistics_bins_hourly);";
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error("[Database] Failed To Connect with database.Closing Java Program."+CommonHelper.getStackTraceString(e));
            System.exit(0);
        }
        catch (Exception ex) {
            logger.error("[Database] Failed To get Data from database.Closing Java Program."+CommonHelper.getStackTraceString(ex));
            System.exit(0);
        }
    }
    public static void createDatabaseConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            logger.info("[Database] Mysql Driver Not Found.Closing Java Program.");
            System.exit(0);
        }
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:3306/%s" +
                        "?allowPublicKeyRetrieval=true" +
                        "&useSSL=false" +
                        "&useUnicode=true" +
                        "&characterEncoding=utf8" +
                        "&allowMultiQueries=true",
                configIni.getProperty("db_host"),
                configIni.getProperty("db_name"));

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configIni.getProperty("db_username"));
        config.setPassword(configIni.getProperty("db_password"));
        config.setMaximumPoolSize(30);
        config.setConnectionTimeout(300000);
        config.setLeakDetectionThreshold(300000);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        try{
            hikariDataSource = new HikariDataSource(config);
            logger.info("[Database] Connected.");
        }
        catch (Exception ex){
            logger.error("[Database] Connection Failed.Closing Java Program.");
            System.exit(0);
        }
    }
    public static Connection getConnection(){
        try {
            return hikariDataSource.getConnection();
        }
        catch (SQLException ex) {
            logger.error("[Database] Connection Failed.Closing Java Program.");
            logger.error("[Database]"+CommonHelper.getStackTraceString(ex));
            System.exit(0);
            return null;
        }
    }
//    public static Connection getConnection() throws SQLException {
//        String jdbcUrl = String.format("jdbc:mysql://%s:3306/%s" +
//                        "?allowPublicKeyRetrieval=true" +
//                        "&useSSL=false" +
//                        "&useUnicode=true" +
//                        "&characterEncoding=utf8" +
//                        "&allowMultiQueries=true",
//                configIni.getProperty("db_host"),
//                configIni.getProperty("db_name"));
//        return DriverManager.getConnection(jdbcUrl,"root","");
//    }
}
