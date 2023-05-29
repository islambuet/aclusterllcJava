package aclusterllc.javaBase;

import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    public static void main(String[] args) {

        Configurator.initialize(null, "./resources/log4j2.xml");
        Logger logger = LoggerFactory.getLogger(Main.class);

        ConfigurationHelper.loadConfig();

        ApeClientMessageQueueHandler apeClientMessageQueueHandler=new ApeClientMessageQueueHandler();
        apeClientMessageQueueHandler.start();
        MainGui mainGui = new MainGui();

        JSONObject machines=(JSONObject)ConfigurationHelper.dbBasicInfo.get("machines");
        for (String key : machines.keySet()) {
            ApeClient apeClient=new ApeClient((JSONObject) machines.get(key),apeClientMessageQueueHandler);
            apeClient.addApeMessageObserver(mainGui);
            apeClient.start();
        }

        mainGui.startGui();
        System.out.println("Main");
        logger.info("Started");
    }
}
