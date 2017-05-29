package ru.elmsoft.sms.object;

import java.util.Date;

/**
 * 
 * SMS сообщение
 *
 */
public class SMSMessage {
	public static final int MESSAGE_TEXT = 0;

	public static final int MESSAGE_BINARY = 1;
	
	private String sendedID;
	private String messageID;


	private String phoneNumberTo;

	private String phoneNumberFrom;

	private byte[] UDH;

	private byte[] data;

	private String message="";

	private int type=MESSAGE_TEXT ;

	private int expiry;

	private Date expiryDate;


	public byte[] getData() {
		return type != SMSMessage.MESSAGE_BINARY ? null : data;
	}
/**
 * 
 * @return срок действия в часах
 */
	public int getExpiry() {
		return expiry;
	}
/**
 * 
 * @return ид. сообщения
 */
	public String getMessageID() {
		return messageID;
	}
/**
 * 
 * @return адрес отправителя
 */
	public String getPhoneNumberFrom() {
		return phoneNumberFrom;
	}
/**
 * 
 * @return адрес получателя
 */
	public String getPhoneNumberTo() {
		return phoneNumberTo;
	}

/**
 * 
 * @return UDH 
 */
	public byte[] getUDH() {
		return UDH;
	}

	public void setData(byte[] data) {
		this.data = data;
		setType(SMSMessage.MESSAGE_BINARY);
	}


	public void setExpiry(int expiry) {
		this.expiry = expiry;
	}

	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	public void setPhoneNumberFrom(String phoneNumberFrom) {
		this.phoneNumberFrom = phoneNumberFrom;
	}

	public void setPhoneNumberTo(String phoneNumberTo) {
		this.phoneNumberTo = phoneNumberTo;
	}


	public void setUDH(byte[] udh) {
		this.UDH = udh;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getMessage() {
		if (type!=SMSMessage.MESSAGE_TEXT) return null;
        //if (message==null) return "";
		return message;
	}

	public int getType() {
		return type;
	}

	public void setMessage(String message) {
		this.message = message;
		setType(SMSMessage.MESSAGE_TEXT);
	}

	public void setType(int type) {
		this.type = type;
	}
    
    /**
     * 
     * @return идентификатор сообщения назначенный SMSC при отправке. (!!!!)
     */
	
    public String getSendedID() {
        return sendedID;
    }
    
    public void setSendedID(String settedID) {
        this.sendedID = settedID;
    }
    
    public String toString(){
    	return "FROM :" + getPhoneNumberFrom() + "MSG:" + getMessage();
    }
}
