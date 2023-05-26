package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
            logger.error(e.toString());
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
            connection.setAutoCommit(false);
            Statement stmt = connection.createStatement();
            stmt.execute(query);
            connection.commit();
            connection.setAutoCommit(true);
            stmt.close();
        }
        catch (SQLException e) {
            logger.error(e.toString());
        }
        System.out.println(query);
    }

}
