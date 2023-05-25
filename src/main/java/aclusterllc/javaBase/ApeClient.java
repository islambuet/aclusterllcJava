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
import java.util.*;

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
	private final long pingDelayMillis = 5000;

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
					logger.info("Reconnecting Exception."+e.getMessage());
				}
			}
			logger.info("Reconnect End.MachinedId: "+clientInfo.get("machine_id"));
		}).start();
	}
	void startPingThread(){
		pingCounter=0;
		new Thread(() -> {
			System.out.println("Ping Start.MachinedId: "+clientInfo.get("machine_id"));
			while (connectedWithApe) {
				if(pingCounter < 3) {
					//Send syncMessage MSG_ID = 130
					sendBytes(new byte[]{0, 0, 0, (byte) 130, 0, 0, 0, 8});
					pingCounter++;
					//send Text editor notification
					try {
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
			System.out.println("Ping End.MachinedId: "+clientInfo.get("machine_id"));

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
				logger.error("[SEND_MESSAGE_TO_APE] "+e.getMessage());
			}
		}
	}
	public void start() {
		if(!worker.isAlive()){
			worker = new Thread(this);
			try {
				logger.info("Trying to connect  with Ape."+clientInfo.get("machine_id") );
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
				logger.error(e.toString());
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
			logger.error(e.toString());
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
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("object",this);
		while (b.length>7){
			int messageId = CommonHelper.bytesToInt(Arrays.copyOfRange(b, 0, 4));
			int messageLength = CommonHelper.bytesToInt(Arrays.copyOfRange(b, 4, 8));
			//System.out.println(messageId+" "+messageLength+" "+b.length);
			byte[] bodyBytes = null;
			if(messageLength>(b.length)){
				System.out.println("Invalid data length");
				break;
			}
			if(messageLength > 8) {
				bodyBytes = Arrays.copyOfRange(b, 8, messageLength);
			}
			jsonObject.put("messageId",messageId);
			jsonObject.put("bodyBytes",bodyBytes);
			apeClientMessageQueueHandler.addMessageToBuffer(jsonObject);
			b= Arrays.copyOfRange(b, messageLength, b.length);
		}
	}

	public void processMessage(JSONObject jsonObject) {
		//logger.info("Message to process.");

	}
}
