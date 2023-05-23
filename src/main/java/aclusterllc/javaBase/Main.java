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
        System.out.println(ConfigurationHelper.configIni.getProperty("db_name"));

        MainGui mainGui = new MainGui();
        mainGui.startGui();
        System.out.println("Main");
        logger.info("Started");
    }
}
