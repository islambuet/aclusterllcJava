package aclusterllc.javaBase;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;

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
    public void startGui() {

        JFrame frame = new JFrame(projectName+" "+projectVersion);
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void processApeMessage(JSONObject jsonMessage) {
        int messageId=jsonMessage.getInt("messageId");
        ApeClient apeClient= (ApeClient) jsonMessage.get("object");
        if(messageId==30){
            if(apeClient.clientInfo.getInt("machine_id")==1){
                this.pingLabel.setText("\u26AB");
            }
        }
        else if(messageId==130){
            if(apeClient.clientInfo.getInt("machine_id")==1){
                this.pingLabel.setText("");
            }
        }
    }
}