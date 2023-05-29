package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class ApeClientMessageQueueHandler {
    Logger logger = LoggerFactory.getLogger(ApeClient.class);
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
            messageBuffer.put(jsonObject);//put waits if buffer full. add throws exception if buffer full
        } catch (InterruptedException e) {
            logger.error("Exception adding queue."+CommonHelper.getStackTraceString(e));
        }
    }
    public void processMessageFromBuffer(){
        while (true) {
            try {
                JSONObject jsonMessage = messageBuffer.take();//waits if empty
                ApeClient apeClient= (ApeClient) jsonMessage.get("object");
                apeClient.processMessage(jsonMessage);
            }
            catch (InterruptedException e) {
                logger.error("Error in take."+CommonHelper.getStackTraceString(e));
            }
        }
    }


}
