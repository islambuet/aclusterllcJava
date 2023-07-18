package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.String.format;
import static java.lang.Thread.sleep;


public class ApeClient implements Runnable, HmiMessageObserver {
	private Thread worker;

	private Thread pingThread;
	Logger logger = LoggerFactory.getLogger(ApeClient.class);
	JSONObject clientInfo;
	ApeClientMessageQueueHandler apeClientMessageQueueHandler;
	Selector selector;
	private SocketChannel socketChannel;

	boolean reconnectThreadRunning = false;
	boolean connectedWithApe = false;
	private final long reconnectThreadDelayMillis = 5000;
	ByteBuffer buffer = ByteBuffer.allocate(10240000);
	private int pingCounter = 0;
	private final long pingDelayMillis = 2500;
	private final List<ApeMessageObserver> apeMessageObservers = new ArrayList<>();

	public ApeClient(JSONObject clientInfo, ApeClientMessageQueueHandler apeClientMessageQueueHandler) {
		this.clientInfo=clientInfo;
		this.apeClientMessageQueueHandler=apeClientMessageQueueHandler;
		worker = new Thread(this);
		//pingThread = new Thread(this::runPingThread);
		start3minTpuThread();
	}
	private void start3minTpuThread() {
		//send a test signal
		// You may or may not want to stop the thread here
		new Thread(() -> {
			LocalDateTime now = LocalDateTime.now();
			int secToWait=181-(((now.getMinute())%3)*60+now.getSecond());
			while (true){
				try {
					Thread.sleep(secToWait * 1000);
					secToWait = 180;
					if (connectedWithApe)
					{
						Connection connection=ConfigurationHelper.getConnection();
						String query=format("SELECT MAX(total_read) max_total_read FROM statistics WHERE machine_id=%d AND created_at>= (SELECT created_at FROM statistics_counter ORDER BY id DESC LIMIT 1);", clientInfo.getInt("machine_id"));
						JSONArray queryResult=DatabaseHelper.getSelectQueryResults(connection,query);
						int maxtput=0;
						if(queryResult.length()>0){
							JSONObject maxResult= queryResult.getJSONObject(0);
							if(maxResult.has("max_total_read")){
								maxtput=maxResult.getInt("max_total_read")*20;
							}
						}
						byte[] messageBytes = new byte[]{0, 0, 0, 126, 0, 0, 0, 12,(byte) (maxtput >> 24), (byte) (maxtput >> 16), (byte) (maxtput >> 8), (byte) (maxtput)};
						sendBytes(messageBytes);
						connection.close();
					}
				}
				catch (Exception ex) {
					logger.error("Max Tput sender Thread Error");
					logger.error(CommonHelper.getStackTraceString(ex));
				}
			}
		}).start();
	}
	void startReconnectThread(){
		new Thread(() -> {
			while(!connectedWithApe) {
				logger.info("Reconnecting.MachinedId: "+clientInfo.get("machine_id"));
				start();
				try {
					sleep(reconnectThreadDelayMillis);
				}
				catch (InterruptedException e) {
					logger.info("Reconnecting Exception."+e);
				}
			}
			logger.info("Reconnect End.MachinedId: "+clientInfo.get("machine_id"));
		}).start();
	}
	void startPingThread(){
		pingCounter=0;
		new Thread(() -> {
			//System.out.println("Ping Start.MachinedId: "+clientInfo.get("machine_id"));
			while (connectedWithApe) {
				if(pingCounter < 3) {
					//Send syncMessage MSG_ID = 130
					sendBytes(new byte[]{0, 0, 0, (byte) 130, 0, 0, 0, 8});

					pingCounter++;
					//send Text editor notification
					try {
						sleep(pingDelayMillis);
						JSONObject jsonObject=new JSONObject();
						jsonObject.put("messageId",130);
						jsonObject.put("messageLength",8);
						jsonObject.put("object",this);
						notifyToApeMessageObservers(jsonObject,new JSONObject());
						sleep(pingDelayMillis);
					}
					catch (InterruptedException e) {
						logger.error("Interrupted ping");
					}
				}
				else {
					disconnectConnectedApeServer();
					break;
				}
			}
			//System.out.println("Ping End.MachinedId: "+clientInfo.get("machine_id"));

		}).start();
	}
	public void sendBytes(byte[] myByteArray)  {

		if (!connectedWithApe)
		{
			logger.error("[SEND_MESSAGE_TO_APE] Ape Server not connected");
		}
		else {
			ByteBuffer buf = ByteBuffer.wrap(myByteArray);
			try {
				socketChannel.write(buf);
			}
			catch (IOException e) {
				logger.error("[SEND_MESSAGE_TO_APE] "+CommonHelper.getStackTraceString(e));
			}
		}
	}
	public void start() {
		if(!worker.isAlive()){
			worker = new Thread(this);
			try {
				logger.info("Trying to connect  with Ape - "+clientInfo.get("machine_id") );
				selector = Selector.open();
				InetSocketAddress inetSocketAddress = new InetSocketAddress(clientInfo.getString("ip_address"), clientInfo.getInt("port_number"));
				socketChannel = SocketChannel.open(inetSocketAddress);
				socketChannel.configureBlocking(false);
				socketChannel.register(selector, SelectionKey.OP_READ, new StringBuffer());
				ConfigurationHelper.apeClientConnectionStatus.put(clientInfo.getInt("machine_id"), 1);
				reconnectThreadRunning=false;
				worker.start();
			}
			catch (IOException e) {
				if(!reconnectThreadRunning){
					reconnectThreadRunning=true;
					ConfigurationHelper.apeClientConnectionStatus.put(clientInfo.getInt("machine_id"), 0);
					startReconnectThread();
				}
				logger.error(e.toString());
			}
		}

	}
	public void run(){
		connectedWithApe=true;
		//Send syncMessage MSG_ID = 116
		sendBytes(new byte[]{0, 0, 0, 116, 0, 0, 0, 8});
		//startPingThread();
		while (connectedWithApe){
			try {
				//System.out.println("waiting for connect or message");
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if(!key.isValid()){
						continue;
					}
					if (key.isReadable()) {
						readReceivedDataFromAPe(key);
					}
				}
			}
			catch (IOException e) {
				logger.error(CommonHelper.getStackTraceString(e));
			}
		}
	}
	public void readReceivedDataFromAPe(SelectionKey key){
		SocketChannel connectedApeServer = (SocketChannel) key.channel();
		buffer.clear();
		int numRead = 0;
		try {
			numRead = connectedApeServer.read(buffer);
		} catch (IOException e) {
			logger.error(CommonHelper.getStackTraceString(e));
			disconnectConnectedApeServer();
			return;
		}
		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel.
			disconnectConnectedApeServer();
			return;
		}
		byte[] b = new byte[buffer.position()];
		buffer.flip();
		buffer.get(b);
		processReceivedDataFromAPe(b);

	}
	public void disconnectConnectedApeServer() {
		try {
			socketChannel.close();
			logger.error("Disconnected From Server");
		}
		catch (IOException e) {
			logger.error(e.toString());
		}
		ConfigurationHelper.apeClientConnectionStatus.put(clientInfo.getInt("machine_id"), 0);
		connectedWithApe=false;
		if(!reconnectThreadRunning){
			reconnectThreadRunning=true;
			startReconnectThread();
		}
		worker.interrupt();
	}

	public void processReceivedDataFromAPe(byte[] b){
		while (b.length>7){
			JSONObject jsonObject=new JSONObject();
			jsonObject.put("object",this);
			int messageId = CommonHelper.bytesToInt(Arrays.copyOfRange(b, 0, 4));
			int messageLength = CommonHelper.bytesToInt(Arrays.copyOfRange(b, 4, 8));

			byte[] bodyBytes = null;
			if(messageLength>(b.length)){
				System.out.println("Invalid data length");
				break;
			}
			if(messageLength > 8) {
				bodyBytes = Arrays.copyOfRange(b, 8, messageLength);
				jsonObject.put("bodyBytes",bodyBytes);
			}
			jsonObject.put("messageId",messageId);
			jsonObject.put("messageLength",messageLength);

			apeClientMessageQueueHandler.addMessageToBuffer(jsonObject);
			b= Arrays.copyOfRange(b, messageLength, b.length);
		}
	}
	public void addApeMessageObserver(ApeMessageObserver apeMessageObserver){
		apeMessageObservers.add(apeMessageObserver);
	}
	public void notifyToApeMessageObservers(JSONObject jsonMessage,JSONObject info){
		//int messageId=jsonMessage.getInt("messageId");
		for(ApeMessageObserver apeMessageObserver:apeMessageObservers){
			//System.out.println(apeMessageObserver.getClass().getSimpleName());
			//limit messageId for others class
			apeMessageObserver.processApeMessage(jsonMessage,info);
		}
	}
	public void processMessage(JSONObject jsonMessage) {
		JSONObject info=new JSONObject();
		int messageId=jsonMessage.getInt("messageId");
		int messageLength=jsonMessage.getInt("messageLength");
		if(messageLength>8){
			try {
				byte[] bodyBytes= (byte[]) jsonMessage.get("bodyBytes");
				//byte[] timestampBytes = Arrays.copyOfRange(bodyBytes, 0, 4);
				byte[] dataBytes = Arrays.copyOfRange(bodyBytes, 4, bodyBytes.length);
				Connection connection=ConfigurationHelper.getConnection();
				//Server >> Client Messages
				switch (messageId){
					case 1:
						ApeClientHelper.handleMessage_1(connection,clientInfo,dataBytes);
						break;
					case 2:
						ApeClientHelper.handleMessage_2(connection,clientInfo,dataBytes);
						break;
					case 3:
						ApeClientHelper.handleMessage_3(connection,clientInfo,dataBytes);
						break;
					case 4:
					case 5:
						ApeClientHelper.handleMessage_4_5(connection,clientInfo,dataBytes,messageId);
						break;
					case 6:
					case 8:
					case 10:
					case 12:
					case 17:
					case 40:
						ApeClientHelper.handleMessage_6_8_10_12_17_40(connection,clientInfo,dataBytes,messageId);
						break;
					case 7:
					case 9:
					case 11:
					case 13:
					case 18:
					case 41:
						ApeClientHelper.handleMessage_7_9_11_13_18_41(connection,clientInfo,dataBytes,messageId);
						break;
					case 14:
						ApeClientHelper.handleMessage_14(connection,clientInfo,dataBytes);
						break;
					case 15:
						ApeClientHelper.handleMessage_15(connection,clientInfo,dataBytes);
						break;
					case 20:
						info=ApeClientHelper.handleMessage_20(connection,clientInfo,dataBytes);
						break;
					case 21:
						info=ApeClientHelper.handleMessage_21(connection,clientInfo,dataBytes);
						break;
					case 22:
						info=ApeClientHelper.handleMessage_22(connection,clientInfo,dataBytes);
						break;
					case 42:
						ApeClientHelper.handleMessage_42(connection,clientInfo,dataBytes);
						break;
					case 43:
						ApeClientHelper.handleMessage_43(connection,clientInfo,dataBytes);
						break;
					case 44:
						info=ApeClientHelper.handleMessage_44(connection,clientInfo,dataBytes);
						break;
					case 45:
						//nothing doing. Receiving only event Id TODO For 360
						break;
					case 46:
						ApeClientHelper.handleMessage_46(connection,clientInfo,dataBytes);
						break;
					case 47:
						ApeClientHelper.handleMessage_47(connection,clientInfo,dataBytes);
						break;
					case 48:
						ApeClientHelper.handleMessage_48(connection,clientInfo,dataBytes);
						break;
					case 49:
						ApeClientHelper.handleMessage_49(connection,clientInfo,dataBytes);
						break;
					case 50:
						//nothing doing. Receiving only estop state and location. TODO For 360
						break;
					case 51:
						//nothing doing. Receiving only reason. TODO For 360
						break;
					case 52:
						//nothing doing. Receiving only speed. TODO For 360
						break;
					case 53:
						ApeClientHelper.handleMessage_53(connection,clientInfo,dataBytes);
						break;
					case 54:
						ApeClientHelper.handleMessage_54(connection,clientInfo,dataBytes);
						break;
					case 55:
						ApeClientHelper.handleMessage_55(connection,this,dataBytes);
						break;
					case 56:
						ApeClientHelper.handleMessage_56(connection,clientInfo,dataBytes);
						break;
					case 57:
						ApeClientHelper.handleMessage_57(connection,clientInfo,dataBytes);
						break;
				}
				//Client >> Server
				//MSG_ID = 115
				//MSG_ID = 111 Missing doc
				//MSG_ID = 115
				//MSG_ID = 120
				//MSG_ID = 123
				//MSG_ID = 124
				//MSG_ID = 125
				connection.close();
			}
			catch (Exception ex){
				logger.error("[MESSAGE_PROCESS]"+CommonHelper.getStackTraceString(ex));
			}
		}
		//MSG_LENGTH = 8
		else {
			switch(messageId) {
				case 16:
					break;
				case 30:
					pingCounter=0;
					break;
				case 58:
					Runtime r = Runtime.getRuntime();
					try
					{
						logger.info("Shutting down after 2 seconds.");
						r.exec("shutdown -s -t 2");
					}
					catch (IOException ex) {
						logger.error(CommonHelper.getStackTraceString(ex));
					}
					break;
				default:
					// code block
			}
			//Client >> Server
			//MSG_ID = 101
			//MSG_ID = 102
			//MSG_ID = 103
			//MSG_ID = 103
			//MSG_ID = 105
			//MSG_ID = 106
			//MSG_ID = 107
			//MSG_ID = 108
			//MSG_ID = 109
			//MSG_ID = 110
			//MSG_ID = 112
			//MSG_ID = 113
			//MSG_ID = 114
			//MSG_ID = 116
			//MSG_ID = 130
		}
//		if(messageId==20|| messageId==21|| messageId==22){
//			System.out.println(messageId+" : "+ info);
//		}
		notifyToApeMessageObservers(jsonMessage,info);

	}

	@Override
	public void processHmiMessage(JSONObject jsonMessage, JSONObject info) {
		try {
			String request = jsonMessage.getString("request");
			JSONObject params = jsonMessage.getJSONObject("params");
			int machine_id=0;
			if(params.has("machine_id")){machine_id=params.getInt("machine_id");}
			if (request.equals("forward_ape_message")) {
				if(machine_id==clientInfo.getInt("machine_id")){
					int message_id = Integer.parseInt(params.get("message_id").toString());
					byte[] messageBytes;
					switch(message_id) {
						case 115: {
							int param_id = Integer.parseInt(params.get("param_id").toString());
							int value = Integer.parseInt(params.get("value").toString());
							messageBytes = new byte[]{
									0, 0, 0, 115, 0, 0, 0, 20, 0, 0, 0, 0,
									(byte) (param_id >> 24), (byte) (param_id >> 16), (byte) (param_id >> 8), (byte) (param_id),
									(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) (value)
							};
							sendBytes(messageBytes);
							break;
						}
						case 120:{
							int mode = Integer.parseInt(params.get("mode").toString());
							messageBytes = new byte[]{
									0, 0, 0, 120, 0, 0, 0, 9, (byte) mode
							};
							sendBytes(messageBytes);
							break;
						}
						case 123:{
							int device_id = Integer.parseInt(params.get("device_id").toString());
							int command = Integer.parseInt(params.get("command").toString());
							int parameter1 = Integer.parseInt(params.get("parameter1").toString());
							if(device_id==86 && command ==0){
								//FOR all machine or current machine
								Connection connection=ConfigurationHelper.getConnection();
								String query= format("INSERT INTO statistics_counter (`machine_id`) VALUES (%d);",machine_id);
								query+= format("INSERT INTO statistics_oee (`machine_id`) VALUES (%d);",machine_id);
								query+=format("INSERT INTO statistics_bins_counter (machine_id,bin_id) SELECT DISTINCT machine_id,bin_id FROM bins WHERE machine_id=%d;",machine_id);
								DatabaseHelper.runMultipleQuery(connection,query);
								connection.close();
							}
							messageBytes= new byte[]{
									0, 0, 0, 123, 0, 0, 0, 20,
									(byte) (device_id >> 24),(byte) (device_id >> 16),(byte) (device_id >> 8),(byte) (device_id),
									(byte) (command >> 24),(byte) (command >> 16),(byte) (command >> 8),(byte) (command),
									(byte) (parameter1 >> 24),(byte) (parameter1 >> 16),(byte) (parameter1 >> 8),(byte) (parameter1)
							};
							sendBytes(messageBytes);
							break;
						}


					}
				}
			}
		}
		catch (Exception ex){
			logger.error(CommonHelper.getStackTraceString(ex));
		}
	}
}
