package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static java.lang.String.format;

public class DatabaseHelper {
    static Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);
    public static void runMultipleQuery(Connection connection,String query) throws SQLException {
        if(query.length()>0){
            connection.setAutoCommit(false);
            Statement stmt = connection.createStatement();
            stmt.execute(query);
            connection.commit();
            connection.setAutoCommit(true);
            stmt.close();
        }
    }
    public static JSONArray getActiveAlarms(Connection connection,int machineId) {
        JSONArray resultsJsonArray = new JSONArray();
        try {
            Statement stmt = connection.createStatement();
            String query = String.format("SELECT *,UNIX_TIMESTAMP(date_active) AS date_active_timestamp FROM active_alarms WHERE machine_id=%d ORDER BY id DESC", machineId);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numColumns = rsMetaData.getColumnCount();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                resultsJsonArray.put(item);
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            logger.error(e.toString());
        }
        return resultsJsonArray;
    }
    public static JSONObject getInputStates(Connection connection,int machineId){
        JSONObject resultJsonObject = new JSONObject();
        try {
            Statement stmt = connection.createStatement();
            String query = String.format("SELECT id,input_id, state FROM input_states WHERE machine_id=%d", machineId);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numColumns = rsMetaData.getColumnCount();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                resultJsonObject.put(machineId+"_"+rs.getString("input_id"),item);
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            logger.error(e.toString());
        }
        return resultJsonObject;
    }
    public static JSONObject getBinStates(Connection connection,int machineId){
        JSONObject resultJsonObject = new JSONObject();
        try {
            Statement stmt = connection.createStatement();
            String query = String.format("SELECT * FROM bin_states WHERE machine_id=%d", machineId);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numColumns = rsMetaData.getColumnCount();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                resultJsonObject.put(machineId+"_"+rs.getString("bin_id"),item);
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            logger.error(e.toString());
        }
        return resultJsonObject;
    }

}
