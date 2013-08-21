package ru.elmsoft.sms.transport;

import java.util.Properties;

import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.SMSMessage;

import com.objectxp.msg.GsmSmsService;
import com.objectxp.msg.GsmStatus;
import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageEventListener;
import com.objectxp.msg.MessageException;
import com.objectxp.msg.SmsMessage;
import com.objectxp.msg.SmsService;
import com.objectxp.msg.StatusReportMessage;

/**
 * 
 * “ранспорт осущетсвл€ющий SMS обмен при помощи GSM-модема
 * 
 */
public class GSMTransport extends AbstractSMSTransport implements MessageEventListener {
	public GSMTransport(Properties props) throws Exception {
		super(props);

	}

	public GSMTransport() throws Exception {
		super();
		createService();
	}

	protected GsmSmsService service;// = new GsmSmsService();

	/**
	 * см. AbstractSMSTransport.init
	 */

	public void init(Properties props) throws MessageException {
		synchronized (getService()) {
			super.init(props);
			processInitParams(props, "gsm.");
			startService();
			setExternalStopFlag(false);
		}
	}

	protected void processStatusReport(MessageEvent event) {
		StatusReportMessage msg = (StatusReportMessage) event.getMessage();
		DeliveryReport report = new DeliveryReport();
		report.setPermanentError(msg.getStatus().isPermanentError());
		report.setDelivered(msg.getStatus().isDelivered());
		report.setMessageId(msg.getID());
		GsmStatus status = (GsmStatus) msg.getStatus();
		logger.info("Report!!!! " + msg.getMessage() + " " + status.toString());
		switch (status.getReasonCode()) {
		case GsmStatus.SMS_RECEIVED_BY_RECIPIENT:
			notifyDeliveryReport(msg, report, "dlr.SMS_RECEIVED_BY_RECIPIENT",
			/* status.getReasonCode() */1);
			break;
		case GsmStatus.SMS_FORWARDED_TO_RECIPIENT:
			notifyDeliveryReport(msg, report, "dlr.SMS_FORWARDED_TO_RECIPIENT",
			/* status.getReasonCode() */4);
			break;
		case GsmStatus.SMS_REPLACED_BY_SC:
			notifyDeliveryReport(msg, report, "dlr.SMS_REPLACED_BY_SC", /*
																		 * status
																		 * .getReasonCode()
																		 */4);
			break;
		case GsmStatus.CONGESTION:
			notifyDeliveryReport(msg, report, "dlr.CONGESTION", /*
																 * status
																 * .getReasonCode()
																 */4);
			break;
		case GsmStatus.RECIPIENT_BUSY:
			notifyDeliveryReport(msg, report, "dlr.RECIPIENT_BUSY", /*
																	 * status
																	 * .getReasonCode()
																	 */2);
			break;
		case GsmStatus.NO_RESPONSE_FROM_RECIPIENT:
			notifyDeliveryReport(msg, report, "dlr.NO_RESPONSE_FROM_RECIPIENT",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.SERVICE_REJECTED:
			notifyDeliveryReport(msg, report, "dlr.SERVICE_REJECTED", /*
																		 * status
																		 * .getReasonCode()
																		 */2);
			break;
		case GsmStatus.QUALITY_NOT_AVAILABLE:
			notifyDeliveryReport(msg, report, "dlr.QUALITY_NOT_AVAILABLE",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.ERROR_IN_RECIPIENT:
			notifyDeliveryReport(msg, report, "dlr.ERROR_IN_RECIPIENT", /*
																		 * status
																		 * .getReasonCode()
																		 */2);
			break;
		case GsmStatus.REMOTE_PROCEDURE_ERROR:
			notifyDeliveryReport(msg, report, "dlr.REMOTE_PROCEDURE_ERROR",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.INCOMPATIBLE_DESTINATION:
			notifyDeliveryReport(msg, report, "dlr.INCOMPATIBLE_DESTINATION",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.CONNECTION_REJECTED_BY_SMSC:
			notifyDeliveryReport(msg, report, "dlr.CONNECTION_REJECTED_BY_SMSC", /* status.getReasonCode() */2);
			break;
		case GsmStatus.NOT_OPTIONABLE:
			notifyDeliveryReport(msg, report, "dlr.NOT_OPTIONABLE", /*
																	 * status
																	 * .getReasonCode()
																	 */2);
			break;
		case GsmStatus.QUALITY_OF_SERVICE_NA:
			notifyDeliveryReport(msg, report, "dlr.QUALITY_OF_SERVICE_NA",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.NO_INTERWORKING_AVAILABLE:
			notifyDeliveryReport(msg, report, "dlr.NO_INTERWORKING_AVAILABLE",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.SMS_VALIDITY_EXPIRED:
			notifyDeliveryReport(msg, report, "dlr.SMS_VALIDITY_EXPIRED",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.SMS_DELETED_BY_ORIGINATOR:
			notifyDeliveryReport(msg, report, "dlr.SMS_DELETED_BY_ORIGINATOR",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.SMS_DELETED_BY_SMSC_ADMIN:
			notifyDeliveryReport(msg, report, "dlr.SMS_DELETED_BY_SMSC_ADMIN",
			/* status.getReasonCode() */2);
			break;
		case GsmStatus.SMS_NOT_EXISTS:
			notifyDeliveryReport(msg, report, "dlr.SMS_NOT_EXISTS", /*
																	 * status
																	 * .getReasonCode()
																	 */2);
			break;
		case GsmStatus.UNKNOWN_ERROR:
			notifyDeliveryReport(msg, report, "dlr.UNKNOWN_ERROR", /*
																	 * status
																	 * .getReasonCode()
																	 */2);
		}
	}

	protected synchronized SmsService getService() {
		createService();
		return service;
	}

	protected void createService() {
		if (service == null)
			service = new GsmSmsService();
	}

	protected void dropService() {
		service = null;

	}

	protected void setAdressParams(SMSMessage message, SmsMessage msg)
			throws EPoisonedMessageException {
		super.setAdressParams(message, msg);
		if (message.getPhoneNumberFrom() != null)
			msg.setSender(message.getPhoneNumberFrom());
	}

	
}
