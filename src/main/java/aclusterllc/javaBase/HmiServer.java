package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class HmiServer implements Runnable {
    private Thread worker;
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    ByteBuffer buffer = ByteBuffer.allocate(10240000);
    ConcurrentHashMap<SocketChannel, JSONObject> connectedHmiClientList = new ConcurrentHashMap<>();
    Logger logger = LoggerFactory.getLogger(HmiServer.class);
    public HmiServer() {
    }
    public void sendMessage(SocketChannel connectedHmiClient, String msg) {
        String startTag="<begin>";
        String endTag="</begin>";
        msg=startTag+msg+endTag;
        ByteBuffer buf = ByteBuffer.wrap(msg.getBytes());
        try {
            connectedHmiClient.write(buf);
        }
        catch (IOException ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }
    public void start(){
        logger.info("HMI Server Started");
        worker = new Thread(this);
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(Integer.parseInt(ConfigurationHelper.configIni.getProperty("hmi_server_port"))));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            worker.start();
        } catch (IOException ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }

    public void run() {
        while (true) {
            try {
                System.out.println("waiting for event");
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if(!key.isValid()){
                        continue;
                    }
                    if(key.isAcceptable()){
                        System.out.println("in accept");
                        registerConnectedHmiClient(key);
                    }
                    else if (key.isReadable()) {
                        System.out.println("in Read");
                        readReceivedDataFromConnectedHmiClient(key);
                    }
                }
            }
            catch (IOException ex) {
                logger.error(CommonHelper.getStackTraceString(ex));
                //e.printStackTrace();
            }
        }

    }
    public void registerConnectedHmiClient(SelectionKey key){
        try {

            SocketChannel connectedHmiClient=serverSocketChannel.accept();
            //TODO add condition form MULTIPLE_CLIENT handling
            connectedHmiClient.configureBlocking(false);
            connectedHmiClient.register(selector, SelectionKey.OP_READ);
            JSONObject connectedHmiClientInfo=new JSONObject();
            connectedHmiClientInfo.put("ipAddress",connectedHmiClient.getRemoteAddress().toString().split("/")[1]);
            connectedHmiClientInfo.put("buffer","");
            connectedHmiClientList.put(connectedHmiClient,connectedHmiClientInfo);
            logger.info("Connected with HmiClient: " + connectedHmiClient.getRemoteAddress());
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }
    public void readReceivedDataFromConnectedHmiClient(SelectionKey key) {
        SocketChannel connectedHmiClient = (SocketChannel) key.channel();
        buffer.clear();
        int numRead = 0;
        try {
            numRead = connectedHmiClient.read(buffer);
        } catch (IOException e) {
            logger.error(e.toString());
            disconnectConnectedHmiClient(connectedHmiClient);
            return;
        }
        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel.
            disconnectConnectedHmiClient(connectedHmiClient);
            return;
        }

        byte[] b = new byte[buffer.position()];
        buffer.flip();
        buffer.get(b);
        processReceivedDataFromConnectedHmiClient(connectedHmiClient,b);
    }
    public void disconnectConnectedHmiClient(SocketChannel connectedHmiClient) {
        try {
            connectedHmiClient.close();
            JSONObject connectedHmiClientInfo= connectedHmiClientList.remove(connectedHmiClient);
            logger.error("Disconnected connectedHmiClient: " + connectedHmiClientInfo.get("ipAddress"));

        } catch (IOException ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }
    public void disconnectAllConnectedHmiClient(){
        for (SocketChannel key : connectedHmiClientList.keySet()) {
            this.disconnectConnectedHmiClient(key);
        }
    }
    public void processReceivedDataFromConnectedHmiClient(SocketChannel connectedHmiClient,byte[] b){
        JSONObject connectedHmiClientInfo= connectedHmiClientList.get(connectedHmiClient);
        if(connectedHmiClientInfo==null){
            logger.error("[DATA_PROCESS] ConnectedHmiClientInfo Not found.");
        }
        else{

            String previousBuffer=(String) connectedHmiClientInfo.get("buffer");
            String data=previousBuffer+new String( b, StandardCharsets.UTF_8 );

            String startTag="<begin>";
            String endTag="</begin>";

            int startPos=data.indexOf(startTag);
            int endPos=data.indexOf(endTag);
            while (startPos>-1 && endPos>-1){
                if(startPos>0){
                    logger.warn("[DATA_PROCESS][START_POS_ERROR] Message did not started with begin. Data: "+data);
                }
                if(startPos>endPos){
                    logger.warn("[DATA_PROCESS][END_POS_ERROR] End tag found before start tag. Data: "+data);
                    data=data.substring(startPos);
                }
                else{
                    String messageString=data.substring(startPos+startTag.length(),endPos);
                    try {
                        JSONObject jsonObject = new JSONObject(messageString);
                            processReceivedMessageFromConnectedHmiClient(connectedHmiClient, jsonObject);
                    }
                    catch (JSONException ex) {
                        logger.error(CommonHelper.getStackTraceString(ex));
                    }
                    data=data.substring(endPos+endTag.length());
                    //parse
                }
                startPos=data.indexOf(startTag);
                endPos=data.indexOf(endTag);
            }
            connectedHmiClientInfo.put("buffer",data);
        }
    }
    public void processReceivedMessageFromConnectedHmiClient(SocketChannel connectedHmiClient,JSONObject jsonObject){
        try {
            JSONObject response=new JSONObject();
            System.out.println(jsonObject);
            String request = jsonObject.getString("request");
            JSONObject params = jsonObject.getJSONObject("params");
            JSONArray requestData = jsonObject.getJSONArray("requestData");

            response.put("request",request);
            response.put("params",params);

            int machine_id=0;
            if(params.has("machine_id")){machine_id=params.getInt("machine_id");}

            if(requestData.length()>0){

            }
            else if (request.equals("basic_info")) {
                response.put("data",ConfigurationHelper.dbBasicInfo);
                sendMessage(connectedHmiClient,response.toString());
            }
            //notify

        }
        catch (Exception ex){
            ex.printStackTrace();
            //logger.error(CommonHelper.getStackTraceString(ex));
        }

    }
}
