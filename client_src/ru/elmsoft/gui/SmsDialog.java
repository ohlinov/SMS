package ru.elmsoft.gui;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.swing.*;

import ru.elmsoft.sms.object.*;
import ru.elmsoft.sms.transport.EPoisonedMessageException;
import ru.elmsoft.sms.transport.SMPPTransport;
import ru.elmsoft.sms.transport.SMSTransport;

import com.objectxp.msg.MessageException;


public class SmsDialog extends javax.swing.JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JButton InitButton;

    private JPanel jPanel1;

    private JTextField numberPhoneEdit;

    private JLabel numPhoneLabel;

    private JPanel ParamPanel;

    private JButton sendButton;

    private JPanel ButtonPanel;

    private JTextArea logArea;

    private JButton exitButton;

    private JTextField smsTextEdit;

    private JLabel SMSTextLabel;

    private JScrollPane ScrollPane;

    private JPanel jPanel2;

    private JTabbedPane TabbedPane;

    private SMSTransport transport = null;

	private JButton stopButton;

    /**
     * Auto-generated main method to display this JDialog
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        SmsDialog inst = new SmsDialog(frame);
        inst.setVisible(true);
    }

    public SmsDialog(JFrame frame) {
        // super(frame);
        initGUI();
    }

    private void initGUI() {
        try {
            BorderLayout thisLayout = new BorderLayout();
            getContentPane().setLayout(thisLayout);
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                        if (transport != null)transport.stop();
                        System.exit(0);
                }
            });

            {
                TabbedPane = new JTabbedPane();
                getContentPane().add(TabbedPane, BorderLayout.CENTER);
                TabbedPane.setPreferredSize(new java.awt.Dimension(394, 237));
                {
                    jPanel1 = new JPanel();
                    TabbedPane.addTab("Log", null, jPanel1, null);
                    jPanel1.setLayout(new BorderLayout());
                    {
                        ScrollPane = new JScrollPane();
                        jPanel1.add(ScrollPane, BorderLayout.CENTER);
                        {
                            logArea = new JTextArea();
                            ScrollPane.setViewportView(logArea);
                        }
                    }
                }
                {
                    jPanel2 = new JPanel();
                    TabbedPane.addTab("Message", null, jPanel2, null);
                    BorderLayout jPanel2Layout = new BorderLayout();
                    jPanel2.setLayout(jPanel2Layout);
                    jPanel2.setPreferredSize(new java.awt.Dimension(-15, 50));
                    {
                        ParamPanel = new JPanel();
                        GridLayout ParamPanelLayout = new GridLayout(2, 2);
                        ParamPanelLayout.setHgap(5);
                        ParamPanelLayout.setVgap(5);
                        ParamPanelLayout.setColumns(2);
                        ParamPanelLayout.setRows(2);
                        ParamPanel.setLayout(ParamPanelLayout);
                        jPanel2.add(ParamPanel, BorderLayout.NORTH);
                        ParamPanel.setPreferredSize(new java.awt.Dimension(366, 41));
                        {
                            numPhoneLabel = new JLabel();
                            ParamPanel.add(numPhoneLabel);
                            numPhoneLabel
                                .setText("\u041d\u043e\u043c\u0435\u0440 \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0430");
                        }
                        {
                            numberPhoneEdit = new JTextField();
                            ParamPanel.add(numberPhoneEdit);
                            numberPhoneEdit.setText("+79272892252");
                        }
                        {
                            SMSTextLabel = new JLabel();
                            ParamPanel.add(SMSTextLabel);
                            SMSTextLabel
                                .setText("\u0422\u0435\u043a\u0441\u0442 SMS");
                        }
                        {
                            smsTextEdit = new JTextField();
                            ParamPanel.add(smsTextEdit);
                            smsTextEdit
                                .setText("\u041e\u0442\u043b\u0430\u0434\u043a\u0430 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b!!! \u0418\u0437\u0432\u0438\u043d\u0438\u0442\u0435 \u0437\u0430 \u0431\u0435\u0441\u043f\u043e\u043a\u043e\u0439\u0441\u0442\u0432\u043e");
                            smsTextEdit
                                .setPreferredSize(new java.awt.Dimension(
                                    188,
                                    93));
                        }
                    }
                    // jPanel2.setTabTitle("\u041f\u0430\u0440\u0430\u043c\u0435\u0442\u0440\u044b");
                }
            }
            {
                ButtonPanel = new JPanel();
                getContentPane().add(ButtonPanel, BorderLayout.SOUTH);
                {
                    InitButton = new JButton();
                    ButtonPanel.add(InitButton);
                    InitButton.setLayout(null);
 
                    InitButton.addActionListener(new ActionListener() {
                        public void actionPerformed(
                                java.awt.event.ActionEvent evt) {
                            // System.out.print("Init button clicked");
                            Properties initProps = new Properties();
                            try {
                                initProps.load(new FileInputStream(new File(
                                        "init.properties")));
                            if (transport != null){
                                transport.stop();
                                transport.init(initProps);
                                return;
                            }
                           // transport = new SMPPTransport();// GSMTransport();//new GSMTransport();
                           

                              //  transport = new GSMTransport(initProps);
                                transport = new SMPPTransport();
                                //transport.stop();
                                transport.init(initProps);
                                transport
                                        .addDeliveryReportListener(new DeliveryReportListener() {
                                            public void receiveDeliveryReport(
                                                    DeliveryReport message) {
                                                logArea
                                                        .setText(logArea
                                                                .getText()
                                                                + "\n DeliveryReport recieve for message with id"
                                                                + message
                                                                        .getMessageId()
                                                                + " state code "
                                                                + message
                                                                        .getStateNumeric()
                                                                + ' '
                                                                + message
                                                                        .getStateString());
                                            }

                                        });
                                
                                transport
                                        .addIncomeSMSListener(new IncomeSMSListener() {
                                            public void receiveIncomeSMS(
                                                    SMSMessage message) {
                                                logArea
                                                        .setText(logArea
                                                                .getText()
                                                                + "\n IncomeMessage recieve"
                                                                + message
                                                                        .getPhoneNumberFrom()
                                                                + ' '
                                                                + message
                                                                        .getMessage());
                                            }
                                        });
                               
                                transport
                                        .addSystemReportListener(new SystemReportListener() {
                                            public void receiveSystemReport(
                                                    SystemReport message) {
                                                logArea
                                                        .setText(logArea
                                                                .getText()
                                                                + "\n System event recieve"
                                                                + message
                                                                        .getCode()
                                                                + ' '
                                                                + message
                                                                        .getDescription());
                                            /*    if (message.getStatus() == -1){
                                                    try {
                                                        
                                                        Properties initProps = new Properties();
                                                        logArea.setText(logArea.getText() + "\n RESTART!!!!!");
                                                       // initProps.load(new FileInputStream(new File("init.properties")));
                                                        
                                                        transport.stop();
                                                        transport.init(initProps);
                                                    } catch (Exception err) {
                                                        logArea.setText(logArea.getText()
                                                                + "\n Error on sending message"
                                                                + err.getMessage());
                                                        err.printStackTrace();
                                                    }
                                                }*/

                                            }
                                        });
                               // transport.init(initProps);
                            } catch (Exception err) {
                                err.printStackTrace();
                            }
                        }
                    });

                    InitButton.setText("InitComponent");
                }
                {
                    sendButton = new JButton();
                    ButtonPanel.add(sendButton);
                    sendButton.setText("Send");
                    sendButton
                            .setPreferredSize(new java.awt.Dimension(104, 22));
                    sendButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            if (transport == null)
                                return;
                            SMSMessage message = new SMSMessage();
                            try {
                                message
                                        .setPhoneNumberFrom("1");
                                message.setPhoneNumberTo(numberPhoneEdit
                                        .getText());
                               // message.set
                                message.setExpiry(1);
                                message.setMessage(smsTextEdit.getText());
                                message.setMessageID("100001");
                                try {
									transport.sendSMSMessage(message);
								} catch (EPoisonedMessageException e) {
									e.printStackTrace();
								}
                            } catch (MessageException err) {
                                err.printStackTrace();
                                logArea.setText(logArea.getText()
                                        + "\n Error on sending message"
                                        + err.getMessage());

                            }
                        }
                    });
                }
                {
                    exitButton = new JButton();
                    ButtonPanel.add(exitButton);
                    exitButton.setText("Exit");
                    exitButton.setPreferredSize(new java.awt.Dimension(92, 22));
                    exitButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            if (transport != null)transport.stop();
                            System.exit(0);
                        }
                    });
                }
                {
                    stopButton = new JButton();
                    ButtonPanel.add(stopButton);
                    stopButton.setText("Stop");
                    stopButton.setPreferredSize(new java.awt.Dimension(92, 22));
                    stopButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            if (transport != null)transport.stop();
                            
                        }
                    });
                }
            }
            setSize(500, 300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
