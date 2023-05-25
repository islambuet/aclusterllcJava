package aclusterllc.javaBase;

import org.json.JSONObject;

public interface ApeMessageObserver {
    public void processApeMessage(JSONObject jsonMessage);
}
