package aclusterllc.javaBase;

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
import java.util.*;

import static java.lang.String.format;
import static java.lang.Thread.sleep;


public class ApeClient implements Runnable {
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
						notifyToApeMessageObservers(130,jsonObject,new JSONObject());
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
				logger.error(CommonHelper.getStackTraceString(e));
			}
		}

	}
	public void run(){
		connectedWithApe=true;
		//Send syncMessage MSG_ID = 116
		sendBytes(new byte[]{0, 0, 0, 116, 0, 0, 0, 8});
		startPingThread();
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
	public void notifyToApeMessageObservers(int messageId,JSONObject jsonMessage,JSONObject info){
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
						ApeClientHelper.handleMessage_6_8(connection,clientInfo,dataBytes,messageId);
						break;
					case 7:
					case 9:
						ApeClientHelper.handleMessage_7_9(connection,clientInfo,dataBytes,messageId);
						break;

				}
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
				default:
					// code block
			}
		}

		//no need to notify for all
//		List<Integer> notifyMessages=Arrays.asList(1,30);
//		if(notifyMessages.contains(messageId)){
//			notifyToApeMessageObservers(jsonMessage);
//		}

		notifyToApeMessageObservers(messageId,jsonMessage,info);

	}
}
