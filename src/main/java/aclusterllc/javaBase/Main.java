package aclusterllc.javaBase;

import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
    public static void main(String[] args) {

        Configurator.initialize(null, "./resources/log4j2.xml");
        Logger logger = LoggerFactory.getLogger(Main.class);

        ConfigurationHelper.loadIniConfig();

        MainGui mainGui = new MainGui();
        mainGui.startGui();
        try {
            int initialSleepTime = Integer.parseInt((ConfigurationHelper.configIni.getProperty("initial_sleep_time")));
            mainGui.appendToMainTextArea("Waiting "+initialSleepTime+"s");
            logger.info("Waiting "+initialSleepTime+"s");
            Thread.sleep(initialSleepTime * 1000);
        }
        catch (InterruptedException e) {
            logger.info("Waiting Error");
            e.printStackTrace();
        }
        mainGui.appendToMainTextArea("Waiting Finished");
        ConfigurationHelper.setSystemConstants();
        ConfigurationHelper.loadDatabaseConfig();
        mainGui.appendToMainTextArea("Database Loading Finished");

        ApeClientMessageQueueHandler apeClientMessageQueueHandler=new ApeClientMessageQueueHandler();
        apeClientMessageQueueHandler.start();




        HmiServer hmiServer=new HmiServer();

        JSONObject machines=(JSONObject)ConfigurationHelper.dbBasicInfo.get("machines");
        for (String key : machines.keySet()) {
            ApeClient apeClient=new ApeClient((JSONObject) machines.get(key),apeClientMessageQueueHandler);
            apeClient.addApeMessageObserver(mainGui);
            apeClient.start();
        }


        hmiServer.start();
        System.out.println("Main");
        logger.info("Started");
    }
}
