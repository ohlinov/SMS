package ru.elmsoft.sms.object;
/**
 * 
 * отчет о доставке
 *
 */
public class DeliveryReport {
	private String MessageId;

	private int StateNumeric;

	private String StateString;

	private boolean IsDelivered;

	private boolean IsPermanentError;
/**
 * 
 * @return true если сообщение доставленно
 */
	public boolean isDelivered() {
		return IsDelivered;
	}
/**
 * 
 * @return true если отчет содержит ошибку
 */
	public boolean isPermanentError() {
		return IsPermanentError;
	}
/**
 * 
 * @return идентифатор сообщения, относительно которого пришел отчет
 */
	public String getMessageId() {
		return MessageId;
	}
/**
 * 
 * @return код отчета
 */
	public int getStateNumeric() {
		return StateNumeric;
	}
/**
 * 
 * @return словесное содержание отчета
 */
	public String getStateString() {
		return StateString;
	}

	public void setDelivered(boolean isDelivered) {
		IsDelivered = isDelivered;
	}

	public void setPermanentError(boolean isPermanentError) {
		IsPermanentError = isPermanentError;
	}

	public void setMessageId(String messageId) {
		MessageId = messageId;
	}

	public void setStateNumeric(int stateNumeric) {
		StateNumeric = stateNumeric;
	}

	public void setStateString(String stateString) {
		StateString = stateString;
	}
	
	public String toString(){
		return "Delivery report" + getStateString() +  " MSG_ID " + getMessageId();
	}
}
