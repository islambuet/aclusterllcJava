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
    public static JSONArray getSelectQueryResults(Connection connection,String query){
        JSONArray resultsJsonArray = new JSONArray();
        try {
            Statement stmt = connection.createStatement();

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
            logger.error(CommonHelper.getStackTraceString(e));
        }
        return resultsJsonArray;
    }
    public static int runUpdateQuery(Connection connection,String query) throws SQLException {
        int num_row=0;
        if(query.length()>0){
            connection.setAutoCommit(false);
            Statement stmt = connection.createStatement();
            num_row = stmt.executeUpdate(query);
            connection.commit();
            connection.setAutoCommit(true);
            stmt.close();
        }
        return num_row;
    }
    public static JSONObject getSelectQueryResults(Connection connection,String query,String[] keyColumns){
        JSONObject resultJsonObject = new JSONObject();
        try {
            Statement stmt = connection.createStatement();
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
                String key="";
                for(int i=0;i<keyColumns.length;i++)
                {
                    if(i==0){
                        key=rs.getString(keyColumns[i]);
                    }
                    else{
                        key+=("_"+rs.getString(keyColumns[i]));
                    }
                }
                resultJsonObject.put(key,item);
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
        return resultJsonObject;
    }
    public static JSONArray getActiveAlarms(Connection connection,int machineId) {
        String query = String.format("SELECT *,UNIX_TIMESTAMP(date_active) AS date_active_timestamp FROM active_alarms WHERE machine_id=%d ORDER BY id DESC", machineId);
        return  getSelectQueryResults(connection,query);
    }
    public static JSONObject getBinStates(Connection connection,int machineId){
        String query = String.format("SELECT * FROM bin_states WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "bin_id"});
    }
    public static JSONObject getConveyorStates(Connection connection,int machineId){
        String query = String.format("SELECT * FROM conveyor_states WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "conveyor_id"});
    }
    public static JSONObject getDeviceStates(Connection connection,int machineId){
        String query = String.format("SELECT * FROM device_states WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "device_id"});
    }
    public static int getDisconnectedDeviceCounter(Connection connection,int machineId){
        int totalDisconnected=0;
        String query = String.format("SELECT COUNT(id) AS totalDisconnected FROM device_states WHERE machine_id=%d AND state=0 AND device_id IN (SELECT device_id FROM devices WHERE machine_id=%d AND gui_id!=0 AND device_id!=1 AND device_id!=2) LIMIT 1", machineId, machineId);
        JSONArray queryResult=getSelectQueryResults(connection,query);
        if(queryResult.length()>0){
            totalDisconnected= queryResult.getJSONObject(0).getInt("totalDisconnected");
        }
        return totalDisconnected;
    }
    public static JSONObject getInductStates(Connection connection,int machineId){
        String query = String.format("SELECT * FROM induct_states WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "induct_id"});
    }
    public static JSONObject getInputStates(Connection connection,int machineId){
        String query = String.format("SELECT * FROM input_states WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "input_id"});
    }
    public static int getMachineMode(Connection connection,int machineId){
        String query = String.format("SELECT machine_mode FROM machines WHERE machine_id=%d", machineId);
        JSONArray modeResult=getSelectQueryResults(connection,query);
        if(modeResult.length()>0){
            return modeResult.getJSONObject(0).getInt("machine_mode");
        }
        return 0;
    }
    public static JSONObject getParameterValues(Connection connection,int machineId){
        String query = String.format("SELECT machine_id,param_id,value FROM parameters WHERE machine_id=%d", machineId);
        return getSelectQueryResults(connection,query,new String[] { "machine_id", "param_id"});
    }

}
