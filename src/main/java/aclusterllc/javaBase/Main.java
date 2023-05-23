package aclusterllc.javaBase;

import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;




public class Main {
    public static void main(String[] args) {

        Configurator.initialize(null, "./resources/log4j2.xml");
        Logger logger = LoggerFactory.getLogger(Main.class);
        MainGui mainGui = new MainGui();
        mainGui.startGui();
        JSONObject jsonObject=new JSONObject();
        System.out.println("Main");
        logger.info("Started");
    }
}
