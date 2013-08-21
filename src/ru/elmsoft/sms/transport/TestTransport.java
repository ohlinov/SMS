package ru.elmsoft.sms.transport;


import java.util.Properties;

import ru.elmsoft.sms.object.*;
import ru.elmsoft.sms.transport.file.CheckFileService;
import ru.elmsoft.sms.transport.file.FileServiceListener;

import com.objectxp.msg.*;


public  class TestTransport extends AbstractSMSTransport {
	public TestTransport(Properties props) throws Exception {
        super(props);
    }

    private CheckFileService service;
	
	private Thread thread;
	
	/* (non-Javadoc)
	 * @see adapter.component.SMSComponent#createSMSMessage(adapter.object.SMSMessage)
	 */
	public  synchronized void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException{
        
	  if (message.getMessage().equalsIgnoreCase("EPoisonedMessageException")) {
		throw new EPoisonedMessageException("EPoisonedMessageException");
	      }	
		if (message == null){
           
		}else{
			createSMSMessageIncome();
            if (message.getMessage().equals("ERROR")) processSystemEvent(new MessageEvent(1, new Message(), this), SystemReport.ERROR, "Message not Sent");
                else{
                    processSystemEvent(new MessageEvent(1, new Message(), this), SystemReport.SUCCESS, "Message Sent"); 
                    createSMSMessageDeliveryReport();
            }
		}
	}

	
	
	private void createSMSMessageIncome(){
        
		SMSMessage message = new SMSMessage();
		message.setData((new String("test Data")).getBytes());
		//message.setSMSC("test SMSC");
		message.setExpiry(1000);
		message.setPhoneNumberFrom("test setPhoneNumberFrom");
		message.setPhoneNumberTo("test setPhoneNumberTo");
		message.setUDH((new String("test UDH")).getBytes());
		message.setMessageID("test messageID");
		logMessage(message, "Test message");
		//return message;
		notifyAllIncomeMessageListener(message);
		//if (listener!=null) listener.createSMSMessageIncome(message);
	}
	
	private void createSMSMessageDeliveryReport(){
        getLogger().info("createSMSMessageDeliveryReport");
		DeliveryReport report = new DeliveryReport();
		report.setStateNumeric(0);
		report.setStateString("test");
		report.setMessageId("1111");
		//logReport(report, "��������� ��������� ������");
		//if (listener_report!=null) listener_report.createSMSMessageDeliveryReport(report);
		notifyAllDLRListener(report);
	}



   /* private void logReport(DeliveryReport report, String comment) {
        getLogger().info(comment);
        getLogger().info("messageID=>" + report.getMessageId());
        getLogger().info("stateNumeric=>" + report.getStateNumeric());
        getLogger().info("Stat�String=>" + report.getStateString());
        getLogger().info("isDelivered=>" + report.isDelivered());
        getLogger().info("isPermanentError=>" + report.isPermanentError());
	}*/

	
	



    protected void processStatusReport(MessageEvent event) {
       
        
    }

    protected SmsService getService() {
        return null;
    }

    public void init(Properties props) throws MessageException{
        super.init(props);
        processInitParams(props, "test.");
        startService();
    }

    protected void startService(){
        service = new CheckFileService();
        service.init(initParams);
        service.addListener( new FileServiceListener(){

            public void fileEvent(boolean isExist) {
                if (!isExist)
                    processSystemEvent(new MessageEvent(1, new Message(), this), SystemReport.ERROR, "File not Exist");
                else 
                    processSystemEvent(new MessageEvent(1, new Message(), this), SystemReport.SUCCESS, "File  Exist");
                
            }
            
        });
        thread = new Thread(service);
        thread.start();
    }
    
    public void stop() {
        //thread.yield();
    }
  
    protected void createService() {
        // TODO Auto-generated method stub
        
    }



	protected void dropService() {
		// TODO Auto-generated method stub
		
	}
	
}
