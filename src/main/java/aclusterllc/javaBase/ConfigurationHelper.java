package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ConfigurationHelper {
    public static final Properties configIni = new Properties();
    static Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);
    public static void loadConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
            for (Map.Entry<Object, Object> entry : configIni.entrySet()) {
                System.out.println(entry.getKey()+"--"+entry.getValue());

            }
        }
        catch (IOException e)
        {
            logger.info("File Config Read failed");
            System.exit(0);
        }
    }
}
//
// try{
//         Properties p = new Properties();
//         p.load(new FileInputStream("user.props"));
//         System.out.println("user = " + p.getProperty("DBuser"));
//         System.out.println("password = " + p.getProperty("DBpassword"));
//         System.out.println("location = " + p.getProperty("DBlocation"));
//         p.list(System.out);
//         }
//         catch (Exception e) {
//         System.out.println(e);
//         }