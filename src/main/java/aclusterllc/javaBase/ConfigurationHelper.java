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
    public static final Map<Integer, String> apeMessageName  = new HashMap<>();


    public static void loadConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
        }
        catch (IOException e)
        {
            logger.info("File Config Read failed"+e);
            System.exit(0);
        }
        setApeMessageName();
        createDatabaseConnection();
        try {
            Connection connection=getConnection();
            Statement stmt = connection.createStatement();
            String query = "SELECT * FROM alarms";
            ResultSet rs = stmt.executeQuery(query);
            JSONObject results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("alarm_id",rs.getString("alarm_id"));
                item.put("alarm_type",rs.getString("alarm_type"));
                item.put("alarm_class",rs.getString("alarm_class"));
                item.put("gui_id",rs.getString("gui_id"));
                item.put("variable_name",rs.getString("variable_name"));
                item.put("description",rs.getString("description"));
                item.put("location",rs.getString("location"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("alarm_id")+"_"+rs.getString("alarm_type"),item);
            }
            dbBasicInfo.put("alarms",results);

            query = "SELECT * FROM bins";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("bin_id",rs.getString("bin_id"));
                item.put("sort_manager_id",rs.getString("sort_manager_id"));
                item.put("description",rs.getString("description"));
                item.put("bin_label",rs.getString("bin_label"));
                item.put("gui_id",rs.getString("gui_id"));
                item.put("recirc_bin",rs.getString("recirc_bin"));
                item.put("reject_bin",rs.getString("reject_bin"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("bin_id"),item);
            }
            dbBasicInfo.put("bins",results);

            query = "SELECT * FROM boards";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("board_id",rs.getString("board_id"));
                item.put("board_type",rs.getString("board_type"));
                item.put("location",rs.getString("location"));
                item.put("slot",rs.getString("slot"));
                item.put("description",rs.getString("description"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("board_id"),item);
            }
            dbBasicInfo.put("boards",results);

            query = "SELECT * FROM board_ios";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("board_id",rs.getString("board_id"));
                item.put("change_allowed",rs.getString("change_allowed"));
                item.put("input_id",rs.getString("input_id"));
                item.put("output_id",rs.getString("output_id"));
                item.put("in",rs.getString("in"));
                item.put("description",rs.getString("description"));
                item.put("tag",rs.getString("tag"));
                item.put("terminal",rs.getString("terminal"));
                results.put(rs.getString("id"),item);
            }
            dbBasicInfo.put("board_ios",results);

            query = "SELECT * FROM conveyors";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("conveyor_id",rs.getString("conveyor_id"));
                item.put("conveyor_type",rs.getString("conveyor_type"));
                item.put("conveyor_name",rs.getString("conveyor_name"));
                item.put("conveyor_tag_name",rs.getString("conveyor_tag_name"));
                item.put("gui_id",rs.getString("gui_id"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("conveyor_id"),item);
            }
            dbBasicInfo.put("conveyors",results);

            query = "SELECT * FROM devices";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("device_id",rs.getString("device_id"));
                item.put("device_type",rs.getString("device_type"));
                item.put("device_name",rs.getString("device_name"));
                item.put("ip_address",rs.getString("ip_address"));
                item.put("gui_id",rs.getString("gui_id"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("device_id"),item);
            }
            dbBasicInfo.put("devices",results);

            query = "SELECT * FROM inputs";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("input_id",rs.getString("input_id"));
                item.put("active_state",rs.getString("active_state"));
                item.put("input_type",rs.getString("input_type"));
                item.put("input_name",rs.getString("input_name"));
                item.put("electrical_name",rs.getString("electrical_name"));
                item.put("gui_id",rs.getString("gui_id"));
                item.put("description",rs.getString("description"));
                item.put("enable_history",rs.getString("enable_history"));
                item.put("device_type",rs.getString("device_type"));
                item.put("device_number",rs.getString("device_number"));
                item.put("device_fct",rs.getString("device_fct"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("input_id"),item);
            }
            dbBasicInfo.put("inputs",results);

            query = "SELECT * FROM machines";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("machine_name",rs.getString("machine_name"));
                item.put("ip_address",rs.getString("ip_address"));
                item.put("port_number",rs.getString("port_number"));
                item.put("site_name",rs.getString("site_name"));
                item.put("maintenance_gui_ip",rs.getString("maintenance_gui_ip"));
                item.put("machine_mode",rs.getString("machine_mode"));
                item.put("machine_state",rs.getString("machine_state"));
                item.put("install_at",rs.getString("install_at"));
                results.put(rs.getString("machine_id"),item);
                for(int i=0;i<32;i++){
                    countersCurrentValue.put(rs.getString("machine_id")+"_"+(i+1),0);
                }
            }
            dbBasicInfo.put("machines",results);

            query = "SELECT * FROM motors";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("motor_id",rs.getString("motor_id"));
                item.put("motor_type",rs.getString("motor_type"));
                item.put("motor_name",rs.getString("motor_name"));
                item.put("electrical_name",rs.getString("electrical_name"));
                item.put("location",rs.getString("location"));
                item.put("ip_address",rs.getString("ip_address"));
                item.put("gui_id",rs.getString("gui_id"));
                item.put("device_number",rs.getString("device_number"));
                item.put("input_id",rs.getString("input_id"));
                item.put("alarm_id",rs.getString("alarm_id"));
                item.put("alarm_type",rs.getString("alarm_type"));
                item.put("speed_max",rs.getString("speed_max"));
                item.put("speed_min",rs.getString("speed_min"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("motor_id"),item);
                motorsCurrentSpeed.put(rs.getString("machine_id")+"_"+rs.getString("motor_id"),0);
            }
            dbBasicInfo.put("motors",results);

            query = "SELECT * FROM parameters";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("machine_id",rs.getString("machine_id"));
                item.put("param_id",rs.getString("param_id"));
                item.put("description",rs.getString("description"));
                item.put("value",rs.getString("value"));
                item.put("unit",rs.getString("unit"));
                item.put("value_min",rs.getString("value_min"));
                item.put("value_max",rs.getString("value_max"));
                results.put(rs.getString("machine_id")+"_"+rs.getString("param_id"),item);
            }
            dbBasicInfo.put("parameters",results);

            query = "SELECT * FROM scs";
            rs = stmt.executeQuery(query);
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                item.put("id",rs.getString("id"));
                item.put("value",rs.getString("value"));
                item.put("label",rs.getString("label"));
                item.put("color",rs.getString("color"));
                results.put(rs.getString("value"),item);
            }
            dbBasicInfo.put("scs",results);

        }
        catch (SQLException e) {
            logger.error("[Database] Failed To Connect with database.Closing Java Program."+e);
            System.exit(0);
        }
        catch (Exception ex) {
            logger.error("[Database] Failed To get Data from database.Closing Java Program."+ex);
            System.exit(0);
        }
    }
    public static void setApeMessageName(){
        apeMessageName.put(1, "System State Message");
        apeMessageName.put(2, "Inputs Message");
        apeMessageName.put(3, "Input Change Message");
        apeMessageName.put(4, "Errors Message");
        apeMessageName.put(5, "Jams Message");
        apeMessageName.put(6, "ConfirmationPEBlocked Message");
        apeMessageName.put(7, "ConfirmationPEBlocked change Message");
        apeMessageName.put(8, "BinPartiallyFull Message");
        apeMessageName.put(9, "BinPartiallyFull change Message");
        apeMessageName.put(10, "BinFull Message");
        apeMessageName.put(11, "BinFull change Message");
        apeMessageName.put(12, "BinDisabled Message");
        apeMessageName.put(13, "BinDisabled change Message");
        apeMessageName.put(14, "Devices Connected Message");
        apeMessageName.put(15, "Device Connected Change Message");
        apeMessageName.put(16, "Sync Response");
        apeMessageName.put(17, "TrayMissing Message");
        apeMessageName.put(18, "TrayMissing change Message");
        apeMessageName.put(20, "Dimension Message");
        apeMessageName.put(21, "Barcode Result");
        apeMessageName.put(22, "ConfirmDestination Message");
        apeMessageName.put(30, "Ping Response");
        apeMessageName.put(40, "BinMode Message");
        apeMessageName.put(41, "BinModechange Message");
        apeMessageName.put(42, "ConveyorState Message");
        apeMessageName.put(43, "ConveyorStateChange Message");
        apeMessageName.put(44, "SensorHit Message");
        apeMessageName.put(45, "Event Message");
        apeMessageName.put(46, "InductLineState Message");
        apeMessageName.put(47, "InductLineStateChange Message");
        apeMessageName.put(48, "PieceInducted Message");
        apeMessageName.put(49, "Motor Speed Message");
        apeMessageName.put(50, "EStop Message");
        apeMessageName.put(53, "Outputs Message");
        apeMessageName.put(54, "Param Value Message");
        apeMessageName.put(120, "SetMode Message");
        apeMessageName.put(101, "Request Inputs State Message");
        apeMessageName.put(102, "Request Errors Message");
        apeMessageName.put(103, "Request Jams Message");
        apeMessageName.put(105, "Request ConfirmationPEBlocked Message");
        apeMessageName.put(106, "Request BinPartiallyFull Message");
        apeMessageName.put(107, "Request BinFull Message");
        apeMessageName.put(108, "Request BinDisabled Message");
        apeMessageName.put(109, "Request DevicesConnected State Message");
        apeMessageName.put(110, "Request BinMode Message");
        apeMessageName.put(111, "SetBinMode Message");
        apeMessageName.put(112, "Request ConveyorState Message");
        apeMessageName.put(113, "Request TrayMissing Message");
        apeMessageName.put(114, "Request InductLineState Message");
        apeMessageName.put(116, "Sync Request");
        apeMessageName.put(130, "Ping Request");
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
    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}
