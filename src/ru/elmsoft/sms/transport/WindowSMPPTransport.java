package ru.elmsoft.sms.transport;


import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageException;
import com.objectxp.msg.StatusReportMessage;
import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.SMSMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WindowSMPPTransport extends SMPPTransport {
    private SentMessageIdGenerator msgIdGen;
    private Map notSendedMessage;
    private Map sendedMessage;

    public WindowSMPPTransport(Properties props) throws Exception {
        super(props);
    }

    public void init(Properties props) throws MessageException {
        super.init(props);
        notSendedMessage = Collections.synchronizedMap(new HashMap());
        sendedMessage = Collections.synchronizedMap(new HashMap());
    }

    public void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException {
        String id = getSentMessageIdGenerator().generate();
        if (notSendedMessage.containsKey(message.getPhoneNumberTo())){
            getLogger().warn("Предыдущее сообщение на номер " + message.getPhoneNumberTo() + " еще не отправлено. Ждем отправки.");
        	waitWhenMessageSended(message.getPhoneNumberTo());
        }
        notSendedMessage.put(message.getPhoneNumberTo(), id);
        super.sendSMSMessage(message);
        message.setSendedID(id);
    }

    private void waitWhenMessageSended(String phoneNumberTo) {
    	int retryCount = 6;
    	try {			
			while (retryCount > 0) {
				Thread.sleep(250);
				if (!notSendedMessage.containsKey(phoneNumberTo)) return;
                retryCount --;
				getLogger().warn("Ожидание отправки. Предыдущее сообщение на номер "+ phoneNumberTo + " еще не отправлено. До аварийного окончания ожидания осталось " + retryCount + " попыток");

			}
			getLogger().error("Ожидание отправки завершено. Номер "+ phoneNumberTo + " будет удален из таблицы неотправленных сообщений, т.к. это блокирует отправку других сообщений");
		} catch (InterruptedException e) {
			getLogger().error("Ошибка при ожидании отправки сообщения на номер " + phoneNumberTo, e);
			getLogger().error("Номер "+ phoneNumberTo + " будет удален из таблицы неотправленных сообщений");
			notSendedMessage.remove(phoneNumberTo);
		}
	}

	protected DeliveryReport createDeliveryReport(StatusReportMessage msg) {
        DeliveryReport report = super.createDeliveryReport(msg);
        if (sendedMessage.containsKey(msg.getID())) {
            report.setMessageId((String) sendedMessage.get(msg.getID()));
            sendedMessage.remove(msg.getID());
        }
        return report;
    }

    protected SentMessageIdGenerator getSentMessageIdGenerator() {
        if (msgIdGen == null) {
            msgIdGen = new SentMessageIDGeneratorAsCounter();
        }
        return msgIdGen;
    }

    protected void onMessageSentEvent(MessageEvent event, boolean isSent) {
        if (isSent) {
            String generatedMessageId = (String) notSendedMessage.get(event.getMessage().getRecipient());
            if (generatedMessageId != null) {
                String id = event.getMessage().getID();
                sendedMessage.put(id, generatedMessageId);
            }
        }
        notSendedMessage.remove(event.getMessage().getRecipient());
    }
}
