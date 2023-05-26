package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;

public class DatabaseHelper {
    static Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);
    public static JSONObject getInputStates(Connection connection,int machineId){
        JSONObject resultJsonObject = new JSONObject();
        try {
            Statement stmt = connection.createStatement();
            String query = String.format("SELECT id,input_id, state FROM input_states WHERE machine_id=%d", machineId);
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next())
            {
                JSONObject row=new JSONObject();
                row.put("id",rs.getInt("id"));
                row.put("input_id",rs.getInt("input_id"));
                row.put("state",rs.getInt("state"));
                row.put("machineId",machineId);
                resultJsonObject.put(machineId+"_"+rs.getString("input_id"),row);
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
