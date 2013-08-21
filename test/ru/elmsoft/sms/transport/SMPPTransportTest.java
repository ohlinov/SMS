package ru.elmsoft.sms.transport;


import com.objectxp.msg.SmsMessage;
import com.objectxp.msg.SmsService;
import com.objectxp.msg.ems.EMSElement;
import com.objectxp.msg.ems.EMSMessage;
import com.objectxp.msg.ems.EMSText;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.mockito.Mockito;
import ru.elmsoft.sms.object.SMSMessage;

import java.util.Properties;

public class SMPPTransportTest extends TestCase {
    private SMPPTransport transport;
    private SmsService mockSms;

    public void setUp() throws Exception {
        mockSms = (SmsService) Mockito.mock(SmsService.class);
        Mockito.when(mockSms.getProperties()).thenReturn(new Properties());
        transport = getMockTransport();
    }

    private SMPPTransport getMockTransport() throws Exception {
        return new SMPPTransport() {
            protected SmsService getService() {
                return mockSms;
            }
        };
    }

    public void testCreateMessageReturnEMSMessage() throws Exception {
        Assert.assertTrue(transport.createMessage(new SMSMessage()) instanceof EMSMessage);
    }

    public void testCreateAsciiShortMessage() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.override.coding.group", "false");
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "non ascii message";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        EMSElement element = transportMessage.getElements()[0];
        Assert.assertTrue(element instanceof EMSText);
        EMSText emsText = (EMSText)element;
        Assert.assertEquals(text, emsText.getText());
        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getAlphabet());
    }

    public void testCreateAsciiMessageWithSpecifiedAlphaBet() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.ascii.text.encoding", ""+SmsMessage.DC_8BIT);
        properties.setProperty("smpp.override.coding.group", "false");

        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "non ascii message";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        Assert.assertEquals(SmsMessage.DC_8BIT, transportMessage.getAlphabet());
        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
    }

    public void testCreateAsciiShortMessageWithSpecifiedDataCoding() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.ascii.message.data.group", ""+SmsMessage.DC_GROUP_DATA);
        properties.setProperty("smpp.override.coding.group", "false");

        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "ascii message";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
    }

    public void testCreateNonAsciiMessageWithSpecifiedDataCoding() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.nonascii.message.data.group", ""+SmsMessage.DC_GROUP_GENERAL);
        properties.setProperty("smpp.override.coding.group", "false");

        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);
        Assert.assertEquals(SmsMessage.DC_GROUP_GENERAL, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
    }

    public void testAutomaticChangeCodingGroupWhenChangeAlphabetToUCS2() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.nonascii.message.data.group", ""+SmsMessage.DC_GROUP_DATA);
        properties.setProperty("smpp.nonascii.text.encoding", ""+SmsMessage.DC_UCS2);
        properties.setProperty("smpp.override.coding.group", "false");

        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);
        Assert.assertEquals(SmsMessage.DC_GROUP_GENERAL, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
    }

    public void testNonAutomaticChangeCodingGroupWhenChangeAlphabetToUCS2() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.nonascii.message.data.group", ""+SmsMessage.DC_GROUP_DATA);
        properties.setProperty("smpp.override.coding.group", "true");
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);
        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
    }

    public void testCreateAsciiMessageForLongText() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "2");  // для ASCII имеются ввиду количество символов
        properties.setProperty("smpp.override.coding.group", "false");
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "ascii message";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        EMSElement element = transportMessage.getElements()[0];
        Assert.assertTrue(element instanceof EMSText);
        EMSText emsText = (EMSText)element;
        Assert.assertEquals(text.substring(0, 2), emsText.getText());

        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_DEFAULT, transportMessage.getAlphabet());
    }

    public void testCreateNonAsciiMessage() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.override.coding.group", "false");
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        EMSElement element = transportMessage.getElements()[0];
        Assert.assertTrue(element instanceof EMSText);
        EMSText emsText = (EMSText)element;
        Assert.assertEquals(text, emsText.getText());
        Assert.assertEquals(SmsMessage.DC_GROUP_GENERAL, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
        Assert.assertEquals(EMSMessage.MT_TEXT, transportMessage.getType());
    }

    public void testCreateNonAsciiMessageWithSpecifiedEncoding() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "100");
        properties.setProperty("smpp.nonascii.text.encoding", ""+SmsMessage.DC_8BIT);
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        Assert.assertEquals(SmsMessage.DC_8BIT, transportMessage.getAlphabet());
        Assert.assertEquals(SmsMessage.DC_GROUP_DATA, transportMessage.getCodingGroup());
    }

    public void testCreateNonAsciiMessageAndTruncateLongText() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("smpp.message.max.length", "2"); // для non-ASCII имеются ввиду байты
        properties.setProperty("smpp.override.coding.group", "false");
        transport.init(properties);

        SMSMessage message = new SMSMessage();
        final String text = "тест";
        message.setMessage(text);

        final EMSMessage transportMessage = (EMSMessage) transport.createMessage(message);

        EMSElement element = transportMessage.getElements()[0];
        Assert.assertTrue(element instanceof EMSText);
        EMSText emsText = (EMSText)element;
        Assert.assertEquals(text.substring(0,1), emsText.getText());
        Assert.assertEquals(SmsMessage.DC_GROUP_GENERAL, transportMessage.getCodingGroup());
        Assert.assertEquals(SmsMessage.DC_UCS2, transportMessage.getAlphabet());
        Assert.assertEquals(EMSMessage.MT_TEXT, transportMessage.getType());
    }
}
