package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.Statement;
//import java.sql.ResultSetMetaData;


public class Test {
    public static void main(String[] args) {
        ConfigurationHelper.loadConfig();
        int start=5;
        String query="";

        for(int i=start;i<start+3;i++){
            query+=format("INSERT INTO products_overwritten (`machine_id`, `mail_id`) VALUES (%d, %d);",1, i);
        }

        try {
           Connection connection = ConfigurationHelper.getConnection();
           connection.setAutoCommit(false);
           Statement stmt = connection.createStatement();
           //System.out.println("stmt Result"+stmt.execute(query,Statement.RETURN_GENERATED_KEYS));
           //System.out.println("stmt Result: "+stmt.executeUpdate(query,Statement.RETURN_GENERATED_KEYS));
            stmt.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
            stmt.executeUpdate("INSERT INTO products (`machine_id`, `mail_id`) VALUES (1, 9);",Statement.RETURN_GENERATED_KEYS);
           ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next())
            {
                System.out.println(rs.getInt(1));
            }
           connection.commit();
           connection.setAutoCommit(true);
           stmt.close();
        }
        catch (Exception ex){
           ex.printStackTrace();

        }
    }
    public static void main3(String[] args) {
        ConfigurationHelper.loadConfig();
        JSONObject x= (JSONObject) ConfigurationHelper.systemConstants.get("APE_MESSAGE_ID_NAME");
        int messageId=1;
        System.out.println(x.get(messageId+""));
        //System.out.println(ConfigurationHelper.systemConstants.get("APE_MESSAGE_ID_NAME").get(1));
    }
    public static void main2(String[] args) {
        BlockingQueue<JSONObject> messageBuffer = new LinkedBlockingQueue<JSONObject>();

        for(int i=5;i<10;i++){
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("messageId",i);
            try {
                messageBuffer.put(jsonObject);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }



        // remove elements from the queue
        // and follow this process again and again
        // until the queue becomes empty
        while (messageBuffer.size() != 0) {

            // Remove Employee item from BlockingQueue
            // using take()
            try {
                JSONObject jsonObject3 = messageBuffer.take();
                System.out.println(jsonObject3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // print removedItem

            int size = messageBuffer.size();

            // print remaining capacity value
            System.out.println("\nSize of list :" + size + "\n");
        }
    }
    public static void main1(String[] args) {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("object","shiafl");
        byte[] bodyBytes = null;
        jsonObject.put("bodyBytes",bodyBytes);

        System.out.println(jsonObject.get("bodyBytes"));

        //System.out.println((byte)130);
    }
}
