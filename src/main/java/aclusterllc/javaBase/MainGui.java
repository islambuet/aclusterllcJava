package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainGui implements ApeMessageObserver {
    public JTextArea mainTextArea;
    private JButton clearButton;
    private JPanel mainPanel;
    private JScrollPane mainScrollPane;
	private JLabel feedLabel;
    public JLabel pingLabel;
    String projectName="Base Java";
    String projectVersion="1.0.1";


    public MainGui() {
        clearButton.addActionListener(actionEvent -> clearMainTextArea());
    }
    public void clearMainTextArea() {
        mainTextArea.setText("");
    }
    public void appendToMainTextArea(String message){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String displayMessage = String.format("[%s] %s",now.format(dateTimeFormatter),message);
        mainTextArea.append(displayMessage+"\r\n");
    }
    public void startGui() {

        JFrame frame = new JFrame(projectName+" "+projectVersion);
        if(Integer.parseInt(ConfigurationHelper.configIni.getProperty("java_server_minimized"))==1){
            frame.setState(Frame.ICONIFIED);
        }
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void processApeMessage(JSONObject jsonMessage, JSONObject info) {
        int messageId=jsonMessage.getInt("messageId");
        ApeClient apeClient= (ApeClient) jsonMessage.get("object");
        if(messageId==30){
            pingLabel.setText("\u26AB");
        }
        else if(messageId==130){
            pingLabel.setText("");
        }
        else{
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String displayMessage = String.format("[%s] :: %s [%s][M:%s].",now.format(dateTimeFormatter),((JSONObject)ConfigurationHelper.systemConstants.get("APE_MESSAGE_ID_NAME")).get(messageId+""),  messageId,apeClient.clientInfo.get("machine_id"));
//                if(info.has("mainGuiMessage")){
//                    mainTextArea.append(info.getString("mainGuiMessage")+"\r\n");
//                }

            int SCROLL_BUFFER_SIZE = 199;
            int numLinesToTrunk = mainTextArea.getLineCount() - SCROLL_BUFFER_SIZE;
            if (numLinesToTrunk > 0) {
                try {
                    int posOfLastLineToTrunk = mainTextArea.getLineEndOffset(numLinesToTrunk - 1);
                    mainTextArea.replaceRange("", 0, posOfLastLineToTrunk);
                }
                catch (BadLocationException ex) {
                    System.out.println(ex.toString());
                }
            }
            mainTextArea.append(displayMessage+"\r\n");
        }

    }
}