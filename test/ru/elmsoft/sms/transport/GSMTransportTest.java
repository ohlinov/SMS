package ru.elmsoft.sms.transport;


import com.objectxp.msg.MessageException;
import com.objectxp.msg.SmsMessage;
import com.objectxp.msg.SmsService;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.mockito.Mockito;
import ru.elmsoft.sms.object.SMSMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class GSMTransportTest extends TestCase {
    private GSMTransport transport;
    private SmsService mockSms;

    public void setUp() throws Exception {
        mockSms = (SmsService) Mockito.mock(SmsService.class);
        Mockito.when(mockSms.getProperties()).thenReturn(new Properties());
        transport = getMockTransport();
    }

    private GSMTransport getMockTransport() throws Exception {
        return new GSMTransport() {
            protected SmsService getService() {
                return mockSms;
            }
        };
    }

    public void testCreateAsciiTextMessage() {
        SMSMessage message = new SMSMessage();
        message.setType(SMSMessage.MESSAGE_TEXT);
        final String messageText = "ASCII text";
        message.setMessage(messageText);

        SmsMessage transportMessage = transport.createMessage(message);
        Assert.assertEquals(messageText, transportMessage.getMessage());
        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getAlphabet());
    }

    public void testCreateNonAsciiTextMessage() {
        SMSMessage message = new SMSMessage();
        message.setType(SMSMessage.MESSAGE_TEXT);
        final String messageText = "тестовое сообщение";
        message.setMessage(messageText);

        SmsMessage transportMessage = transport.createMessage(message);
        Assert.assertEquals(messageText, transportMessage.getMessage());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getAlphabet());
    }


    public void testCreateNonAsciiTextMessageWhenEncodingIsSpecified() throws MessageException, UnsupportedEncodingException {
        Properties properties = new Properties();
        final String charsetName = "UTF-8";
        properties.setProperty("gsm.message.encoding", charsetName);
        transport.init(properties);

        final String messageText = "тестовое сообщение";
        SMSMessage message = new SMSMessage();
        message.setMessage(new String(messageText.getBytes(charsetName), charsetName));

        SmsMessage transportMessage = transport.createMessage(message);
        //Assert.assertNull( transportMessage.getMessage());
        Assert.assertEquals(messageText, new String(transportMessage.getUserData(), charsetName));
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
    }

    public void testCreateNonAsciiTextMessageWhenEncodingIsSpecifiedButNotSupported() throws MessageException,
            UnsupportedEncodingException {
        Properties properties = new Properties();
        final String charsetName = "UTF-8";
        properties.setProperty("gsm.message.encoding", "unsupported");
        transport.init(properties);

        final String messageText = "тестовое сообщение";
        SMSMessage message = new SMSMessage();
        message.setMessage(new String(messageText.getBytes(charsetName), charsetName));

        SmsMessage transportMessage = transport.createMessage(message);
        Assert.assertEquals(messageText, transportMessage.getMessage());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getAlphabet());
    }

    public void testFillMessageId() throws Exception {
        SMSMessage message = new SMSMessage();
        final String messageId = "messageId";
        message.setMessageID(messageId);
        message.setType(SMSMessage.MESSAGE_TEXT);
        message.setMessage("ASCII text");

        SmsMessage transportMessage = transport.createMessage(message);
        Assert.assertEquals(messageId, transportMessage.getID());
    }
}
