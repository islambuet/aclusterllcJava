package aclusterllc.aclusterllcJava;

import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;


public class Main {
    public static void main(String[] args) {
        Configurator.initialize(null, "./resources/log4j2.xml");
        Logger logger = LoggerFactory.getLogger(Main.class);
        System.out.println("Main");
        logger.info("Started");
    }
}
