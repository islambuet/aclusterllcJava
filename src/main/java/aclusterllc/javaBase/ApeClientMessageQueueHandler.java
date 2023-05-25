package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class ApeClientMessageQueueHandler {
    Logger logger = LoggerFactory.getLogger(ApeClientMessageQueueHandler.class);
    //List<JSONObject> messageBuffer = new ArrayList<>();
    private final BlockingQueue<JSONObject> messageBuffer = new LinkedBlockingQueue<JSONObject>();
    public void start(){
        new Thread(() -> {
            try {
                processMessageFromBuffer();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public ApeClientMessageQueueHandler(){

    }
    public void addMessageToBuffer(JSONObject jsonObject){
        try {
            messageBuffer.put(jsonObject);
        } catch (InterruptedException e) {
            logger.error("Exception adding queue."+e.getMessage());
        }
    }
    public void processMessageFromBuffer(){
        while (true) {
            //JSONObject messageObject= null;
            try {
                logger.info("Waiting For Message");
                JSONObject messageObject = messageBuffer.take();//waits if empty
                logger.info("Message Found");
                ApeClient apeClient= (ApeClient) messageObject.get("object");
                apeClient.processMessage(messageObject);
            }
            catch (InterruptedException e) {
                logger.error("Error in take."+e.getMessage());
            }
        }
    }


}
