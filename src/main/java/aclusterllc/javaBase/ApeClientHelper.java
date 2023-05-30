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
    public static void handleMessage_6_8_10_12_17(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binStates=DatabaseHelper.getBinStates(connection,clientInfo.getInt("machine_id"));
        JSONObject bins= (JSONObject) ConfigurationHelper.dbBasicInfo.get("bins");
        String columName="pe_blocked";////messageId=6
        if(messageId==8){
            columName="partially_full";
        }
        else if(messageId==10){
            columName="full";
        }
        else if(messageId==12){
            columName="disabled";
        }
        else if(messageId==17){
            columName="tray_missing";
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
    public static void handleMessage_7_9_11_13_18(Connection connection, JSONObject clientInfo, byte[] dataBytes,int messageId){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject binStates=DatabaseHelper.getBinStates(connection,clientInfo.getInt("machine_id"));
        JSONObject bins= (JSONObject) ConfigurationHelper.dbBasicInfo.get("bins");
        int bin_id = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];
        String columName="pe_blocked";//messageId=7
        if(messageId==9){
            columName="partially_full";
        }
        else if(messageId==11){
            columName="full";
        }
        else if(messageId==13){
            columName="disabled";
        }
        else if(messageId==18){
            columName="tray_missing";
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
        //System.out.println("Query: "+query);
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }
    public static void handleMessage_14(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject devices= (JSONObject) ConfigurationHelper.dbBasicInfo.get("devices");
        JSONObject deviceStates=DatabaseHelper.getDeviceStates(connection,machineId);
        byte []bits=CommonHelper.bitsFromBytes(dataBytes,4);
        String query="";
        for(int i=0;i<bits.length;i++){
            if(deviceStates.has(machineId+"_"+(i+1))){
                JSONObject deviceState= (JSONObject) deviceStates.get(machineId+"_"+(i+1));
                if(deviceState.getInt("state")!=bits[i]){
                    query+= format("UPDATE device_states SET `state`=%d,`updated_at`=now() WHERE id=%d;",bits[i],deviceState.getInt("id"));
                    query+= format("INSERT INTO device_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                }
            }
            else{
                query+= format("INSERT INTO device_states (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
                query+= format("INSERT INTO device_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,(i+1),bits[i]);
            }
        }
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }

    }
    public static void handleMessage_15(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject deviceStates=DatabaseHelper.getDeviceStates(connection,clientInfo.getInt("machine_id"));
        int device_id = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 2));
        int state=dataBytes[2];

        String query="";
        if(deviceStates.has(machineId+"_"+device_id)){
            JSONObject deviceState= (JSONObject) deviceStates.get(machineId+"_"+device_id);
            if(deviceState.getInt("state")!=state){
                query+=format("UPDATE device_states SET `state`='%d', `updated_at`=now()  WHERE `id`=%d;",state,deviceState.getInt("id"));
                query+=format("INSERT INTO device_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
            }
        }
        else{
            query+= format("INSERT INTO device_states (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
            query+= format("INSERT INTO device_states_history (`machine_id`, `device_id`,`state`) VALUES (%d,%d,%d);",machineId,device_id,state);
        }
        try {
            DatabaseHelper.runMultipleQuery(connection,query);
        }
        catch (SQLException e) {
            logger.error(CommonHelper.getStackTraceString(e));
        }
    }
    public static JSONObject handleMessage_20(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject productInfo=new JSONObject();
        long mailId = CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        int length = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8));
        int width = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 8, 12));
        int height = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 12, 16));
        int weight = (int) CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 16, 20));
        int rejectCode=dataBytes[20];
        String queryCheckProduct=format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machineId, mailId);
        JSONArray queryCheckProductResult=DatabaseHelper.getSelectQueryResults(connection,queryCheckProduct);
        if(queryCheckProductResult.length()>0){
            productInfo=queryCheckProductResult.getJSONObject(0);
            String query =format("UPDATE products SET length=%d, width=%d, height=%d, weight=%d, reject_code=%d, dimension_at=NOW() WHERE id=%d;",
                     length, width, height, weight, rejectCode, productInfo.getInt("id"));
            try {
                DatabaseHelper.runMultipleQuery(connection,query);
            }
            catch (SQLException e) {
                logger.error(CommonHelper.getStackTraceString(e));
                productInfo=new JSONObject();//removing info for unSuccess
            }
        }
        else{
            logger.warn("[PRODUCT][20] Product not found found. MailId="+mailId);
        }
        return productInfo;


    }
    public static JSONObject handleMessage_44(Connection connection, JSONObject clientInfo, byte[] dataBytes){
        int machineId=clientInfo.getInt("machine_id");
        JSONObject productInfo=new JSONObject();

        long mailId = CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 0, 4));
        long sensorId = CommonHelper.bytesToLong(Arrays.copyOfRange(dataBytes, 4, 8));
        int sensorStatus=dataBytes[8];
        if((sensorId == 1) && (sensorStatus == 1)) {
            String query="";
            String queryOldProduct=format("SELECT * FROM products WHERE machine_id=%d AND mail_id=%d;", machineId, mailId);
            JSONArray previousProductInfo=DatabaseHelper.getSelectQueryResults(connection,queryOldProduct);
            if(previousProductInfo.length()>0){
                int oldProductId=previousProductInfo.getJSONObject(0).getInt("id");
                logger.info("[PRODUCT][44] Duplicate Product found. MailId="+mailId+" productId="+oldProductId);
                query+=format("INSERT INTO products_overwritten SELECT * FROM products WHERE id=%d;", oldProductId);
                query+=format("DELETE FROM products WHERE id=%d;", oldProductId);
            }
            try {
                connection.setAutoCommit(false);
                Statement stmt = connection.createStatement();
                if(query.length()>0){
                    stmt.execute(query);
                }
                query = format("INSERT INTO products (`machine_id`, `mail_id`) VALUES (%d, %d);",machineId, mailId);
                stmt.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
                ResultSet rs = stmt.getGeneratedKeys();
                if(rs.next())
                {
                    productInfo.put("productId",rs.getLong(1));
                }
                connection.commit();
                connection.setAutoCommit(true);
                rs.close();
                stmt.close();
            }
            catch (Exception ex){
                logger.error("[PRODUCT][44] "+CommonHelper.getStackTraceString(ex));
            }
        }
        else{
            logger.info("[PRODUCT][44] Product not inserted. sensorId="+sensorId+". sensorStatus="+sensorStatus+". MailId="+mailId);
        }
        productInfo.put("mailId",mailId);
        return productInfo;
    }

}
