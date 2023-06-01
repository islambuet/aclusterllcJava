package aclusterllc.javaBase;

import org.json.JSONObject;

public interface HmiMessageObserver {
    public void processHmiMessage(JSONObject jsonMessage,JSONObject info);
}
