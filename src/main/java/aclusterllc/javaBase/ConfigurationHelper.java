package aclusterllc.javaBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ConfigurationHelper {
    public static final Properties configIni = new Properties();
    static Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);
    private static HikariDataSource hikariDataSource;
    public static void loadConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
        }
        catch (IOException e)
        {
            logger.info("File Config Read failed");
            System.exit(0);
        }
        createDatabaseConnection();
        try {
            Connection connection=getConnection();

        } catch (SQLException e) {
            logger.info("[Database] Failed To Connect with database.Closing Java Program.");
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
    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}
