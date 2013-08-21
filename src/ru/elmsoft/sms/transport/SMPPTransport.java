package ru.elmsoft.sms.transport;

import java.util.Properties;

import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.SMSMessage;
import ru.elmsoft.sms.object.SystemReport;
import ru.elmsoft.sms.transport.listener.SmppMultiPartListener;

import com.objectxp.msg.GsmAddress;
import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageEventListener;
import com.objectxp.msg.MessageException;
import com.objectxp.msg.SmppException;
import com.objectxp.msg.SmppSmsService;
import com.objectxp.msg.SmppStatus;
import com.objectxp.msg.SmsMessage;
import com.objectxp.msg.SmsService;
import com.objectxp.msg.StatusReportMessage;
import com.objectxp.msg.ems.EMSMessage;
import com.objectxp.msg.ems.EMSText;

/**
 * 
 * “ранспорт осущетсвл€ющий SMS обмен при помощи SMPP-протокола
 * 
 */
public class SMPPTransport extends AbstractSMSTransport implements SMSTransport, MessageEventListener {

    //private long msfqfullTimeout;
    private long throttlingTimeout;
    private int maxLengthPartMessage;
    private String poisonedCodeList;

    public SMPPTransport(Properties props) throws Exception {
        super(props);
    // service = new SmppSmsService();
    }
    public SMPPTransport() throws Exception {
        super();
        createService();
    // service = new SmppSmsService();
    }
    private SmppSmsService service;

    protected void processStatusReport(MessageEvent event) {
        StatusReportMessage msg = (StatusReportMessage) event.getMessage();
        DeliveryReport report = new DeliveryReport();
        report.setPermanentError(msg.getStatus().isPermanentError());
        report.setDelivered(msg.getStatus().isDelivered());
        report.setMessageId(msg.getID());

        SmppStatus status = (SmppStatus) msg.getStatus();
        logger.info("Report!!!! " + msg.getMessage() + " " + status.toString() + " " + msg.getID());
        switch (status.getMessageState()) {
            case SmppStatus.STATE_ACCEPTED:
                notifyDeliveryReport(msg, report, "dlr.STATE_ACCEPTED", 4); // Message is in accepted state.
                break;

            case SmppStatus.STATE_DELETED:
                notifyDeliveryReport(msg, report, "dlr.STATE_DELETED", 4); // Message has been deleted.
                break;

            case SmppStatus.STATE_DELIVERED:
                notifyDeliveryReport(msg, report, "dlr.STATE_DELIVERED", 1); // Message is delivered to destination
                break;

            case SmppStatus.STATE_ENROUTE:
                notifyDeliveryReport(msg, report, "dlr.STATE_ENROUTE", 2); // The message is in enroute state.
                break;

            case SmppStatus.STATE_EXPIRED:
                notifyDeliveryReport(msg, report, "dlr.STATE_EXPIRED", 2); // Message validity period has expired.
                break;

            case SmppStatus.STATE_REJECTED:
                notifyDeliveryReport(msg, report, "dlr.STATE_REJECTED", 2); // Message is in a rejected state.
                break;

            case SmppStatus.STATE_SCHEDULED:
                notifyDeliveryReport(msg, report, "dlr.STATE_SCHEDULED", 4); // The message is scheduled.
                break;

            case SmppStatus.STATE_SKIPPED:
                notifyDeliveryReport(msg, report, "dlr.STATE_SKIPPED", 4); // The message was accepted but not
                // transmitted on the network.
                break;

            case SmppStatus.STATE_UNDELIVERABLE:
                notifyDeliveryReport(msg, report, "dlr.STATE_UNDELIVERABLE", 2); // Message is undeliverable.
                break;

            case SmppStatus.STATE_UNKNOWN:
                notifyDeliveryReport(msg, report, "dlr.STATE_UNKNOWN", 2); // Message is undeliverable.
        }
    }

    public void handleMessageEvent(MessageEvent event)  {
        if (event.getType() == MessageEvent.MESSAGE_NOT_SENT) {
            if (event.getException() instanceof SmppException) {
                SmppException err = (SmppException) event.getException();
                switch (err.getCommandStatus()) {
                    case 88:
                        return;

                    case 1:
                        return;

                    case 10:
                        return;
                    case 11:
                        return;
                    case 12:
                        return;
                    case 51:
                        return;
                    case 52:
                        return;
                    case 67:
                        return;
                    case 98:
                        return;
                    case 20:
                        return;
                    default:
                        if (checkCustomPoisonedCode(Integer.toString(err.getCommandStatus()))) {
                            return;
                        }
                }
            } //  if (event.getException() instanceof SmppException) 
        }// if (event.getType() == MessageEvent.MESSAGE_SENT) {
        super.handleMessageEvent(event);
    }

    private boolean checkCustomPoisonedCode(String code) {
    	if (poisonedCodeList == null) return false;
    	String codes_split[] = poisonedCodeList.split("\\s*,\\s*");
		for (int i = 0; i<codes_split.length; i ++) {
			if(codes_split[i].trim().equals(code)) return true;
		}
    	return false;
    }
    protected void processSystemEvent(MessageEvent event, int status, String message) {
        getLogger().debug("SMPP System event handler");
        if (event.getException() != null) {
            if (event.getException() instanceof SmppException) {
                SmppException e = (SmppException) event.getException();
                switch (e.getCommandStatus()) {
                    case 88:
                        getLogger().debug("System event not sended " + event.toString());
                        return;
                    case 1:
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 10: /* 0x0A */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 11: /* 0x0B */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 12: /* 0x0C */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 51: /* 0x33 */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 52: /* 0x34 */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 67: /* 0x43 */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 98: /* 0x62 */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    case 20: /* 0x14 */
                        getLogger().debug("Change type System event to INFO " + event.toString());
                        status = SystemReport.INFO;
                        break;
                    default:
                        break;
                }

            }
        }
        // getLogger().debug(
        // "SMMP System event handler dispatch message to external listener");
        getLogger().debug("SMPP System event handler dispatch message to external listener");
        super.processSystemEvent(event, status, message);
    }

    protected void setAdressParams(SMSMessage message, SmsMessage msg) throws EPoisonedMessageException {
        if (message.getPhoneNumberTo() == null) {
            throw new EPoisonedMessageException("Destination address is null ESME_RINVDSTADR");
        }

        if (message.getPhoneNumberFrom() == null) {
            throw new EPoisonedMessageException("Source address is null ESME_RINVSRCADR");
        }
        int destAddressNPI = 1;
        int destAddressTON = 1;
        int sourceAddressNPI = 1;
        int sourceAddressTON = 0;
        if (initParams.getProperty("smpp.destination.ton") != null) {
        	destAddressTON = Integer.parseInt(initParams.getProperty("smpp.destination.ton"));
		} else {
			logger.warn("smpp.smpp.destination.ton not setted in props. This parameter set to default value 1 ");
		}
        if (initParams.getProperty("smpp.destination.npi") != null) {
        	destAddressNPI = Integer.parseInt(initParams.getProperty("smpp.destination.npi"));
		} else {
			logger.warn("smpp.smpp.destination.ton not setted in props. This parameter set to default value 1 ");
		}
        if (initParams.getProperty("smpp.sender.ton") != null) {
        	sourceAddressTON = Integer.parseInt(initParams.getProperty("smpp.sender.ton"));
		} else {
			logger.warn("smpp.smpp.sender.ton not setted in props. This parameter set to default value 0 ");
		}
        if (initParams.getProperty("smpp.sender.npi") != null) {
        	sourceAddressNPI = Integer.parseInt(initParams.getProperty("smpp.sender.npi"));
		} else {
			logger.warn("smpp.smpp.sender.npi not setted in props. This parameter set to default value 1 ");
		}
        //GsmAddress(java.lang.String address, byte TON, byte NPI) 
        GsmAddress destAddress = new GsmAddress(message.getPhoneNumberTo(), (byte) destAddressTON, (byte) destAddressNPI);
        GsmAddress senderAddress = new GsmAddress(message.getPhoneNumberFrom(), (byte) sourceAddressTON, (byte) sourceAddressNPI);

        msg.setSenderAddress(senderAddress);
        msg.setRecipientAddress(destAddress);
    }

    protected SmsMessage createMessage(SMSMessage message) {
        EMSMessage msg = null;
        msg = new EMSMessage();
        if (!message.getMessage().matches("[\\p{ASCII}]*")) {
            msg.setType(EMSMessage.MT_TEXT);
            msg.setAlphabet(SmsMessage.DC_UCS2);
            int truncated_length = (maxLengthPartMessage / 2) > message.getMessage().length() ? message.getMessage().length() : (maxLengthPartMessage / 2);
            msg.add(new EMSText(message.getMessage().substring(0, truncated_length)));
        } else {
            msg.setCodingGroup(SmsMessage.DC_DEFAULT);
            msg.setAlphabet(SmsMessage.DC_DEFAULT);
            int truncated_length = maxLengthPartMessage > message.getMessage().length() ? message.getMessage().length() : maxLengthPartMessage;
            msg.add(new EMSText(message.getMessage().substring(0, truncated_length)));
        }
        return msg;
    }

    protected void finalize() {

    }

    /**
     * »нициализаци€ транспорта. ƒл€ отбора параметров используетс€ префикс smpp
     */
    public void init(Properties props) throws MessageException {
        createService();
        synchronized (getService()) {
            super.init(props);
            processInitParams(props, "smpp.");
            if (props.getProperty("smpp.throttling.timeout") != null) {
                throttlingTimeout = Long.parseLong(props.getProperty("smpp.throttling.timeout"));
            } else {
                throttlingTimeout=60010;
                logger.warn("smpp.throttling.timeout not setted in props. This parameter set to default value 60s");
            }

           /* if (props.getProperty("smpp.mqf.timeout") != null) {
                msfqfullTimeout = Long.parseLong(props.getProperty("smpp.mqf.timeout"));
            } else {
                logger.warn("smpp.mqf.timeout not setted in props. This parameter set to default value 60s");
            }*/

            if (props.getProperty("smpp.message.max.length") != null) {
                maxLengthPartMessage = Integer.parseInt(props.getProperty("smpp.message.max.length"));
            } else {
                maxLengthPartMessage=(140-7)*2;
                logger.warn("smpp.message.max.length not setted in props. This parameter set to default value " + maxLengthPartMessage);
                
            }

            // список кодов ошибок, которые не должны привести к перезагруке (Poisoned Code)
            if (props.getProperty("smpp.message.poisoned.code.list") != null) {
                poisonedCodeList=props.getProperty("smpp.message.poisoned.code.list");
            } else {
                poisonedCodeList="";
                logger.warn("smpp.message.poisoned.code.list not setted in props.");
            }
            startService();

            setExternalStopFlag(false);

        }
    }

    protected SmsService getService() {
        if (service != null) {
            getLogger().debug("Perform operation on service with hashcode " + service.hashCode());
        }
        return service;
    }

    protected void createService() {

        if (service == null) {
            getLogger().debug("Create serice");
            service = new SmppSmsService();
            getLogger().debug("Created service with hashcode " + service.hashCode());
        }else {
        	getLogger().warn("Service created1!!!");
        }
    }

    protected void dropService() {
        getLogger().debug("Drop service " + service.hashCode());
        service = null;
    }

    public synchronized void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException {
        try {
            getLogger().info("Send SMPP message");

            super.sendSMSMessage(message);
        } catch (SmppException err) {
            err.printStackTrace();
            getLogger().error(err);
            getLogger().debug(null, err);
            getLogger().info(
                    "Error on sending SMPP message with type: " + err.getCommandStatus() + " message:" + err.getMessage() + " error type :" + err.getErrorType());

            switch (err.getCommandStatus()) {
                case 88:
                    try {
                        getLogger().debug("THROTTLING_TIMEOUT");
                        getLogger().info("Sleep on " + throttlingTimeout + "ms");
                        stopTimer();
                        Thread.sleep(throttlingTimeout);
                        restartTimer();
                        getLogger().info("Resend message!!!");
                    // sendSMSMessage(message);
                    } catch (InterruptedException err1) {
                        err1.printStackTrace();
                        getLogger().debug(null, err);
                        getLogger().error(err1);
                    }
                    sendSMSMessage(message);
                    break;
                case 1:
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVMSGLEN");
                case 10: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVSRCADR");
                case 11: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVDSTADR");
                case 12: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVMSGID");
                case 51: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVNUMDESTS");
                case 52: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVDLNAME");
                case 67: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVESMCLASS");
                case 98: 
                throw new EPoisonedMessageException(err.getMessage() + " ESME_RINVEXPIRY");
                case 20: 
                throw new EPoisonedMessageException(err.getMessage() + "ESME_RMSGQFUL");
                

                default:
                    if (checkCustomPoisonedCode(Integer.toString(err.getCommandStatus()))) {
                        getLogger().info("Found custom poisoned code: "+Integer.toString(err.getCommandStatus()));
                        throw new EPoisonedMessageException(err.getMessage() + " " + Integer.toString(err.getCommandStatus()));
                    } else {
                        throw err;
                    }
            }

        }
    }

    protected void subscribeListener() {
        SmppMultiPartListener listener = new SmppMultiPartListener();
        listener.addListener(this);
        getService().addMessageEventListener(listener);
    }

   
}
