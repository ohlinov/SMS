package ru.elmsoft.sms.transport;

import java.util.Properties;

import org.mockito.Mockito;

import com.objectxp.msg.Message;
import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageException;
import com.objectxp.msg.SmsService;
import com.objectxp.msg.StatusReportMsg;

import junit.framework.TestCase;
import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.DeliveryReportListener;
import ru.elmsoft.sms.object.SMSMessage;

public class WindowSMPPTransportTest extends TestCase {
	private static final String PHONE_NUMBER_TO = "phoneNumberTo";
	private SentMessageIdGenerator msgIdGen;
	private SMPPTransport transport;
	private SmsService mockSms;

	public void setUp() throws Exception {
		mockSms = (SmsService) Mockito.mock(SmsService.class);
		Mockito.when(mockSms.getProperties()).thenReturn(new Properties());
		transport = getMockTransport();
		msgIdGen = (SentMessageIdGenerator) Mockito.mock(SentMessageIdGenerator.class);
	}

	private SMPPTransport getMockTransport() throws Exception {
		return new WindowSMPPTransport(new Properties()) {
			protected SmsService getService() {
				return mockSms;
			}

			protected SentMessageIdGenerator getSentMessageIdGenerator() {
				return msgIdGen;
			}
		};
	}

	public void test_generate_id_message_when_transport_return_null_msg_id()
			throws MessageException, EPoisonedMessageException {
		SMSMessage msg = makeMessage("");

		String expectedMessageId = "message_id";
		Mockito.when(msgIdGen.generate()).thenReturn(expectedMessageId);

		transport.sendSMSMessage(msg);
		assertEquals(expectedMessageId, msg.getSendedID());
	}

	public void test_send_message_and_decode_id_of_delivery_report()
			throws MessageException, EPoisonedMessageException {
		SMSMessage msg = makeMessage(PHONE_NUMBER_TO);

		final String firstMsgGeneratedId = "message_id";
		Mockito.when(msgIdGen.generate()).thenReturn(firstMsgGeneratedId);

		transport.addDeliveryReportListener(new DeliveryReportListener() {
			public void receiveDeliveryReport(DeliveryReport message) {
				assertEquals(
						"Не смотря на то, что у DLR пришел с одним ид, из транспорта отчет о доставке должен уйти с генерированным ид.",
						firstMsgGeneratedId, message.getMessageId());
			}
		});
		transport.sendSMSMessage(msg);

		Message message = new Message();
		message.setRecipient(PHONE_NUMBER_TO);
		String sentID = "message_send_id";
		message.setID(sentID);

		MessageEvent messageSentEvent = new MessageEvent(MessageEvent.MESSAGE_SENT, message, this);
		transport.handleMessageEvent(messageSentEvent); // послали событие, что
														// сообщение отослано

		MessageEvent dlrReportEvent = new MessageEvent(MessageEvent.STATUS_RECEIVED, new StatusReportMsg(), this);
		dlrReportEvent.getMessage().setID(sentID);
		transport.handleMessageEvent(dlrReportEvent); // послали событие, что
														// пришел Delivery
														// Report. У message id
														// = sentId
	}

	public void test_send_2_messages_to_same_recipient() throws MessageException, EPoisonedMessageException, InterruptedException {
		StringBuffer log = new StringBuffer();
		SMSMessage firstMessage = makeMessage(PHONE_NUMBER_TO);
		SMSMessage secondMessage = makeMessage(PHONE_NUMBER_TO);
		final String firstMsgGeneratedId = "first_message_id";
		final String secondMsgGeneratedId = "second_message_id";
		Mockito.when(msgIdGen.generate()).thenReturn(firstMsgGeneratedId).thenReturn(secondMsgGeneratedId);
		log.append("Послали первое сообщение на номер ").append(PHONE_NUMBER_TO).append("\n");
		transport.sendSMSMessage(firstMessage);
		// определяем поток для отсылки
	    EventFromTransportSender sender = new EventFromTransportSender(transport, "msg1", PHONE_NUMBER_TO, log);
	    Thread thread = new Thread(sender);
		thread.start();
		
		transport.sendSMSMessage(secondMessage);
		log.append("Послали второе сообщение на номер ").append(PHONE_NUMBER_TO);
	    
		Thread.sleep(1500);
	    assertEquals("Второе сообщение должно отправиться на номер, только после того, как отправится первое", 
	    		"Послали первое сообщение на номер phoneNumberTo\n"+
	    		"Отправлено событие MESSAGE_SENT для сообщения recipient: phoneNumberTo"+
	    		"Послали второе сообщение на номер phoneNumberTo",
	    		log.toString());
	}

	public void test_send_2_messages_to_diff_recipient() throws MessageException, EPoisonedMessageException, InterruptedException {
		StringBuffer log = new StringBuffer();
		SMSMessage firstMessage = makeMessage(PHONE_NUMBER_TO);
		SMSMessage secondMessage = makeMessage(PHONE_NUMBER_TO + "_2");
		final String firstMsgGeneratedId = "first_message_id";
		final String secondMsgGeneratedId = "second_message_id";
		Mockito.when(msgIdGen.generate()).thenReturn(firstMsgGeneratedId).thenReturn(secondMsgGeneratedId);
		log.append("Послали первое сообщение на номер ").append(PHONE_NUMBER_TO).append("\n");
		transport.sendSMSMessage(firstMessage);
		// определяем поток для отсылки
		EventFromTransportSender sender = new EventFromTransportSender(transport, "msg1", PHONE_NUMBER_TO, log);
		Thread thread = new Thread(sender);
		thread.start();

		transport.sendSMSMessage(secondMessage);
		log.append("Послали второе сообщение на номер ").append(secondMessage.getPhoneNumberTo());

		Thread.sleep(1500);
		assertEquals("Второе сообщение должно отправиться на номер, сразу, т.к. получатели разные",
				"Послали первое сообщение на номер phoneNumberTo\n"+
						"Послали второе сообщение на номер phoneNumberTo_2" +
						"Отправлено событие MESSAGE_SENT для сообщения recipient: phoneNumberTo",
				log.toString());
	}

	private SMSMessage makeMessage(String phoneNumberTo) {
		SMSMessage msg = new SMSMessage();
		msg.setMessage("тест");
		msg.setPhoneNumberTo(phoneNumberTo);
		msg.setPhoneNumberFrom("");
		return msg;
	}
}

class EventFromTransportSender implements Runnable {
	private SMPPTransport transport;
	private String sentId;
	private String recipientPhone;
	int eventType;
	private StringBuffer log;

	public EventFromTransportSender(SMPPTransport transport, String sentId, String recipientPhone, StringBuffer log) {
		super();
		this.transport = transport;
		this.sentId = sentId;
		this.recipientPhone = recipientPhone;
		eventType = MessageEvent.MESSAGE_SENT;
		this.log = log;
	}


	public void run() {
		MessageEvent messageSentEvent = makeMessage();
		sleep();
		String messageType = eventType == MessageEvent.MESSAGE_SENT ? "MESSAGE_SENT" : "UNKNOWN";
		log.append("Отправлено событие ").append(messageType).append(" для сообщения recipient: ")
				.append(recipientPhone);
		transport.handleMessageEvent(messageSentEvent); // послали событие, что
														// сообщение отослано
	}

	private MessageEvent makeMessage() {
		Message message = new Message();
		message.setRecipient(recipientPhone);
		message.setID(sentId);

		MessageEvent messageSentEvent = new MessageEvent(eventType, message, this);
		return messageSentEvent;
	}

	private void sleep() {
		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}