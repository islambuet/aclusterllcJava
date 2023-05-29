package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static java.lang.String.format;

public class ApeClientHelper {
    static Logger logger = LoggerFactory.getLogger(ApeClient.class);
    public static void handleMessage_1(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        try {
            Statement stmt = connection.createStatement();
            String sql = format("UPDATE machines SET `machine_state`=%d, `machine_mode`=%d, `updated_at`=now()  WHERE `machine_id`=%d LIMIT 1",
                    dataBytes[0],
                    dataBytes[1],
                    clientInfo.getInt("machine_id"));
            stmt.execute(sql);
            stmt.close();
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }
    public static void handleMessage_2(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        JSONObject inputsInfo= (JSONObject) ConfigurationHelper.dbBasicInfo.get("inputs");
        byte []bits=CommonHelper.bitsFromBytes(dataBytes,4);
        int machineId=clientInfo.getInt("machine_id");
        JSONObject inputsCurrentState=DatabaseHelper.getInputStates(connection,machineId);
        String query="";
        for(int i=0;i<bits.length;i++){
            boolean insertHistory=false;
            if(inputsCurrentState.has(machineId+"_"+(i+1))){
                JSONObject inputState= (JSONObject) inputsCurrentState.get(machineId+"_"+(i+1));
                if(inputState.getInt("state")!=bits[i]){
                    query+= format("UPDATE input_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",bits[i],inputState.getInt("id"));
                    insertHistory=true;
                }
            }
            else{
                query+= format("INSERT INTO input_states (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                insertHistory=true;
            }
            if(insertHistory && (inputsInfo.has(machineId+"_"+(i+1))) && (((JSONObject)inputsInfo.get(machineId+"_"+(i+1))).getInt("enable_history")==1)){
                    query+= format("INSERT INTO input_states_history (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
            }
        }
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }

    }
    public static void handleMessage_3(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        JSONObject inputsInfo= (JSONObject) ConfigurationHelper.dbBasicInfo.get("inputs");
        int machineId=clientInfo.getInt("machine_id");
        int inputId = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        try {
            Statement stmt = connection.createStatement();
            String query = String.format("SELECT id,state FROM input_states WHERE machine_id=%d AND input_id=%d", machineId,inputId);
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next())
            {
                if(rs.getInt("state")!=state){
                    String query2= format("UPDATE input_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",state,rs.getInt("id"));
                    if((inputsInfo.has(machineId+"_"+inputId)) && (((JSONObject)inputsInfo.get(machineId+"_"+inputId)).getInt("enable_history")==1)){
                        query2+= format("INSERT INTO input_states_history (`machine_id`, `input_id`,`state`) VALUES (%d,%d,%d);",machineId,inputId,state);
                    }
                    Statement stmt2 = connection.createStatement();
                    stmt2.execute(query2);
                    stmt2.close();

                }
            }
            rs.close();
            stmt.close();
        }
        catch (Exception e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }
    public static void handleMessage_4_5(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId){
        int machineId=clientInfo.getInt("machine_id");
        JSONArray activeAlarms= DatabaseHelper.getActiveAlarms(connection,machineId);
        JSONObject jsonActiveAlarms=new JSONObject();
        int alarm_type=0;////messageId=4
        if(messageId==5){
            alarm_type=1;
        }
        for(int i=0;i<activeAlarms.length();i++){
            JSONObject item= (JSONObject) activeAlarms.get(i);
            if(item.getInt("alarm_type")==alarm_type){
                jsonActiveAlarms.put(item.getInt("machine_id")+"_"+item.getInt("alarm_id"),item);
            }

        }
        byte []bits=CommonHelper.bitsFromBytes(dataBytes,4);
        String query="";

        for(int i=0;i<bits.length;i++){
            if(bits[i]==1){
                if(!(jsonActiveAlarms.has(machineId+"_"+(i+1)))){
                    query+= format("INSERT INTO active_alarms (`machine_id`, `alarm_id`,`alarm_type`) VALUES (%d,%d,%d);",machineId,(i+1),alarm_type);
                }
            }
            else{
                if((jsonActiveAlarms.has(machineId+"_"+(i+1)))){
                    JSONObject item= (JSONObject) jsonActiveAlarms.get(machineId+"_"+(i+1));
                    query+= format("INSERT INTO active_alarms_history (`machine_id`, `alarm_id`,`alarm_type`,`date_active`) VALUES (%d,%d,%d,'%s');"
                            ,machineId,(i+1),alarm_type,item.get("date_active"));
                    query+=format("DELETE FROM active_alarms where id=%d;",item.getInt("id"));
                }

            }
        }
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }

    }
    public static void handleMessage_6_8(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binStates=DatabaseHelper.getBinStates(connection,clientInfo.getInt("machine_id"));
        JSONObject bins= (JSONObject) ConfigurationHelper.dbBasicInfo.get("bins");
        String columName="pe_blocked";////messageId=6
        if(messageId==8){
            columName="partially_full";
        }
        byte []bits=CommonHelper.bitsFromBytes(Arrays.copyOfRange(dataBytes, 4, dataBytes.length),4);//0-3 is number of bins which is equal to bits length
        String query="";
        for(int i=0;i<bits.length;i++){
            int bin_id=(i+1);
            if(bins.has(machineId+"_"+bin_id)){
                if(binStates.has(machineId+"_"+bin_id)){
                    JSONObject binState= (JSONObject) binStates.get(machineId+"_"+bin_id);
                    if(binState.getInt(columName)!=bits[i]){
                        query+=format("UPDATE bin_states SET `%s`='%s', `updated_at`=now()  WHERE `id`=%d;",columName,bits[i],binState.getInt("id"));
                        String unChangedQuery="";
                        for(String key:binState.keySet()){
                            if(!(key.equals("id")|| key.equals("updated_at")|| key.equals(columName)))
                            {
                                unChangedQuery+=format("`%s`='%s',",key,binState.getInt(key));
                            }
                        }
                        query+= format("INSERT INTO bin_states_history SET %s `%s`='%s', `updated_at`=now();",unChangedQuery,columName,bits[i]);
                    }
                }
                else{
                    query+= format("INSERT INTO bin_states (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,bits[i]);
                    query+= format("INSERT INTO bin_states_history (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,bits[i]);
                }
            }

        }
        //System.out.println("Query: "+query);
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }
    public static void handleMessage_7_9(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binStates=DatabaseHelper.getBinStates(connection,clientInfo.getInt("machine_id"));
        JSONObject bins= (JSONObject) ConfigurationHelper.dbBasicInfo.get("bins");
        int bin_id = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String columName="pe_blocked";//messageId=7
        if(messageId==9){
            columName="partially_full";
        }
        String query="";
        if(bins.has(machineId+"_"+bin_id)){
            if(binStates.has(machineId+"_"+bin_id)){
                JSONObject binState= (JSONObject) binStates.get(machineId+"_"+bin_id);
                if(binState.getInt(columName)!=state){
                    query+=format("UPDATE bin_states SET `%s`='%s', `updated_at`=now()  WHERE `id`=%d;",columName,state,binState.getInt("id"));
                    String unChangedQuery="";
                    for(String key:binState.keySet()){
                        if(!(key.equals("id")|| key.equals("updated_at")|| key.equals(columName)))
                        {
                            unChangedQuery+=format("`%s`='%s',",key,binState.getInt(key));
                        }
                    }
                    query+= format("INSERT INTO bin_states_history SET %s `%s`='%s', `updated_at`=now();",unChangedQuery,columName,state);
                }
            }
            else{
                query+= format("INSERT INTO bin_states (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,state);
                query+= format("INSERT INTO bin_states_history (`machine_id`, `bin_id`,`%s`) VALUES (%d,%d,%d);",columName,machineId,bin_id,state);
            }
        }
        System.out.println("Query: "+query);
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }

}
