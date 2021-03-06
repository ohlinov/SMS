package ru.elmsoft.sms.transport;

import com.objectxp.msg.*;
import com.objectxp.msg.ems.EMSMessage;
import com.objectxp.msg.ems.EMSText;
import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.SMSMessage;
import ru.elmsoft.sms.object.SystemReport;
import ru.elmsoft.sms.transport.listener.SmppMultiPartListener;

import java.util.Properties;

public class SMPPTransport extends AbstractSMSTransport implements SMSTransport, MessageEventListener {

    private long throttlingTimeout;
    private int maxLengthPartMessage;
    private String poisonedCodeList;

    public SMPPTransport(Properties props) throws Exception {
        super(props);
    }
    public SMPPTransport() throws Exception {
        super();
        createService();
    }
    private SmppSmsService service;

    protected void processStatusReport(MessageEvent event) {
        StatusReportMessage msg = (StatusReportMessage) event.getMessage();
        DeliveryReport report = createDeliveryReport(msg);

        SmppStatus status = (SmppStatus) msg.getStatus();
        LOGGER.info("Report!!!! " + msg.getMessage() + " " + status.toString() + " " + msg.getID());
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

    protected DeliveryReport createDeliveryReport(StatusReportMessage msg) {
        DeliveryReport report = new DeliveryReport();
        report.setPermanentError(msg.getStatus().isPermanentError());
        report.setDelivered(msg.getStatus().isDelivered());
        report.setMessageId(msg.getID());
        return report;
    }

    public void handleMessageEvent(MessageEvent event)  {
        if (event.getType() == MessageEvent.MESSAGE_NOT_SENT) {
            if (event.getException() instanceof SmppException) {
                SmppException err = (SmppException) event.getException();
                switch (err.getCommandStatus()) {
                    case 88:
                    case 1:
                    case 10:
                    case 11:
                    case 12:
                    case 51:
                    case 52:
                    case 67:
                    case 98:
                    case 20:
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
                    case 10: /* 0x0A */
                    case 11: /* 0x0B */
                    case 12: /* 0x0C */
                    case 51: /* 0x33 */
                    case 52: /* 0x34 */
                    case 67: /* 0x43 */
                    case 98: /* 0x62 */
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
			LOGGER.warn("smpp.smpp.destination.ton not setted in props. This parameter set to default value 1 ");
		}
        if (initParams.getProperty("smpp.destination.npi") != null) {
        	destAddressNPI = Integer.parseInt(initParams.getProperty("smpp.destination.npi"));
		} else {
			LOGGER.warn("smpp.smpp.destination.ton not setted in props. This parameter set to default value 1 ");
		}
        if (initParams.getProperty("smpp.sender.ton") != null) {
        	sourceAddressTON = Integer.parseInt(initParams.getProperty("smpp.sender.ton"));
		} else {
			LOGGER.warn("smpp.smpp.sender.ton not setted in props. This parameter set to default value 0 ");
		}
        if (initParams.getProperty("smpp.sender.npi") != null) {
        	sourceAddressNPI = Integer.parseInt(initParams.getProperty("smpp.sender.npi"));
		} else {
			LOGGER.warn("smpp.smpp.sender.npi not setted in props. This parameter set to default value 1 ");
		}
        //GsmAddress(java.lang.String address, byte TON, byte NPI) 
        GsmAddress destAddress = new GsmAddress(message.getPhoneNumberTo(), (byte) destAddressTON, (byte) destAddressNPI);
        GsmAddress senderAddress = new GsmAddress(message.getPhoneNumberFrom(), (byte) sourceAddressTON, (byte) sourceAddressNPI);

        msg.setSenderAddress(senderAddress);
        msg.setRecipientAddress(destAddress);
    }

    protected SmsMessage createMessage(SMSMessage message) {
        EMSMessage msg = new EMSMessage();
        final int lengthOfTextToSend = message.getMessage().length();
        final boolean containOnlyAsciiChars = isContainOnlyAsciiChars(message.getMessage());
        short codingGroup;
        short alphabet;
        if (!containOnlyAsciiChars) {
            msg.setType(EMSMessage.MT_TEXT);
            codingGroup = getCodingGroupForMessageWithNonASCIIText(SmsMessage.DC_GROUP_DATA);
            alphabet = getAlphabetForMessageWithNonASCIIText(SmsMessage.DC_UCS2);
            int truncated_length = (maxLengthPartMessage / 2) > lengthOfTextToSend
                    ? lengthOfTextToSend
                    : (maxLengthPartMessage / 2);
            msg.add(new EMSText(message.getMessage().substring(0, truncated_length)));
        } else { // ASCII only
            int truncated_length = maxLengthPartMessage > lengthOfTextToSend
                    ? lengthOfTextToSend
                    : maxLengthPartMessage;
            codingGroup = getCodingGroupForMessageWithASCIIText(SmsMessage.DC_GROUP_GENERAL);
            alphabet = getAlphabetForMessageWithASCIIText(SmsMessage.DC_DEFAULT);
            msg.add(new EMSText(message.getMessage().substring(0, truncated_length)));
        }
        msg.setAlphabet(alphabet);
        if (canOverrideCodingGroup()){
            msg.setCodingGroup(codingGroup);
        }
        return msg;
    }

    private boolean canOverrideCodingGroup() {
        return getBooleanValue("override.coding.group", false);
    }

    private boolean getBooleanValue(String key, boolean defaultValue) {
        if (initParams.containsKey(key)) {
            return toBoolean(initParams.getProperty(key));
        } else {
            getLogger().warn("smpp." + key + " not existed in props. This parameter set to default value " + defaultValue);
            return defaultValue;
        }
    }

    boolean toBoolean(String value) {
        return ((value != null) && value.equalsIgnoreCase("true"));
    }


    private short getCodingGroupForMessageWithNonASCIIText(short defaultValue) {
        int result = getIntFromInitValues("nonascii.message.data.group", defaultValue);
        return (short) result;
    }

    private short getCodingGroupForMessageWithASCIIText(short defaultValue) {
        int result = getIntFromInitValues("ascii.message.data.group", defaultValue);
        return (short) result;
    }

    private boolean isContainOnlyAsciiChars(String text) {
        return text.matches("[\\p{ASCII}]*");
    }

    private short getAlphabetForMessageWithNonASCIIText(short defaultValue) {
        int result = getIntFromInitValues("nonascii.text.encoding", defaultValue);
        return (short) result;
    }

    private short getAlphabetForMessageWithASCIIText(short defaultValue) {
        int result = getIntFromInitValues("ascii.text.encoding", defaultValue);
        return (short) result;
    }
    private int getIntFromInitValues(String key, int defaultValue) {
        if (initParams.containsKey(key)) {
            return Integer.parseInt(initParams.getProperty(key));
        } else {
            getLogger().warn("smpp." + key + " not existed in props. This parameter set to default value " + defaultValue);
            return defaultValue;
        }
    }

    public void init(Properties props) throws MessageException {
        createService();
        synchronized (getService()) {
            super.init(props);
            processInitParams(props, "smpp.");
            if (props.getProperty("smpp.throttling.timeout") != null) {
                throttlingTimeout = Long.parseLong(props.getProperty("smpp.throttling.timeout"));
            } else {
                throttlingTimeout=60010;
                LOGGER.warn("smpp.throttling.timeout not setted in props. This parameter set to default value 60s");
            }

           /* if (props.getProperty("smpp.mqf.timeout") != null) {
                msfqfullTimeout = Long.parseLong(props.getProperty("smpp.mqf.timeout"));
            } else {
                LOGGER.warn("smpp.mqf.timeout not setted in props. This parameter set to default value 60s");
            }*/

            if (props.getProperty("smpp.message.max.length") != null) {
                maxLengthPartMessage = Integer.parseInt(props.getProperty("smpp.message.max.length"));
            } else {
                maxLengthPartMessage=(140-7)*2;
                LOGGER.warn("smpp.message.max.length not setted in props. This parameter set to default value " + maxLengthPartMessage);
                
            }

            if (props.getProperty("smpp.message.poisoned.code.list") != null) {
                poisonedCodeList=props.getProperty("smpp.message.poisoned.code.list");
            } else {
                poisonedCodeList="";
                LOGGER.warn("smpp.message.poisoned.code.list not setted in props.");
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

    public void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException {
        try {
            getLogger().info("Start sendSMSMessage");
            super.sendSMSMessage(message);
            getLogger().info("End sendSMSMessage");
        } catch (SmppException err) {
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
