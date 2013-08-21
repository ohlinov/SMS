package ru.elmsoft.sms.transport.billing.file;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ru.elmsoft.sms.object.*;

import com.objectxp.msg.MessageEvent;
/**
 * 
 * —лушатель событий транспорта. ѕредназначен дл€ ведени€ особого вида журналов дл€ 
 * систем биллинга.
 *
 */
public class BaseBillingFile implements SystemReportListener,
	IncomeSMSListener, DeliveryReportListener {
    // private OutputStream stream;
    private Logger logger = Logger.getLogger(BaseBillingFile.class);

    public BaseBillingFile(/* String namefile */) {

    }

    protected void printToLog(String type, String sender, String receiver,
	    String text, String id) {
	char delim = '\t';
/*	Calendar calendar = GregorianCalendar.getInstance(Locale.ENGLISH);
	calendar.*/
	DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	logger.info(new String(format.format(new Date())
		+ delim
		+ type
		+ delim
		+ sender
		+ delim
		+ receiver
		+ delim
		+ text + delim + id));
    }

    public void receiveSystemReport(SystemReport message) {
	MessageEvent event = message.getEvent();
	if (event == null) return;
	if (event.getType() == MessageEvent.MESSAGE_SENT)
	    printToLog("outcome", event.getMessage().getSender(), event
		    .getMessage().getRecipient(), event.getMessage()
		    .getMessage(), event.getMessage().getID());
    }

    public void receiveIncomeSMS(SMSMessage message) {
	printToLog("income", message.getPhoneNumberFrom(), message
		.getPhoneNumberTo(), message.getMessage(), message
		.getMessageID());
    }

    public void receiveDeliveryReport(DeliveryReport message) {
	printToLog("dlr", "NA", "NA", message.getStateString(), message
		.getMessageId());
    }

}
