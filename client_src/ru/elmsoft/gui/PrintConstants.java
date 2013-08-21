package ru.elmsoft.gui;

import com.objectxp.msg.GsmStatus;
import com.objectxp.msg.SmppStatus;

public class PrintConstants {

    /**
     * @param args
     */
    public static void main(String[] args) {
       System.out.println("Delivery report for GSM constants");
       System.out.println( GsmStatus.SMS_RECEIVED_BY_RECIPIENT+ "=> SMS_RECEIVED_BY_RECIPIENT"); 
       System.out.println( GsmStatus.SMS_FORWARDED_TO_RECIPIENT+ "=>> SMS_FORWARDED_TO_RECIPIENT");
       System.out.println( GsmStatus.SMS_REPLACED_BY_SC+ "=>> SMS_REPLACED_BY_SC");
       System.out.println( GsmStatus.CONGESTION+ "=>> CONGESTION");
       System.out.println( GsmStatus.RECIPIENT_BUSY+ "=>> RECIPIENT_BUSY");
       System.out.println( GsmStatus.NO_RESPONSE_FROM_RECIPIENT+ "=>> NO_RESPONSE_FROM_RECIPIENT");
       System.out.println( GsmStatus.SERVICE_REJECTED+ "=>> SERVICE_REJECTED");
       System.out.println( GsmStatus.QUALITY_NOT_AVAILABLE+ "=>> QUALITY_NOT_AVAILABLE");
       System.out.println( GsmStatus.ERROR_IN_RECIPIENT+ "=>> ERROR_IN_RECIPIENT");
       System.out.println( GsmStatus.REMOTE_PROCEDURE_ERROR+ "=>> REMOTE_PROCEDURE_ERROR");
       System.out.println( GsmStatus.INCOMPATIBLE_DESTINATION+ "=>> INCOMPATIBLE_DESTINATION");
       System.out.println( GsmStatus.CONNECTION_REJECTED_BY_SMSC+ "=>> CONNECTION_REJECTED_BY_SMSC");
       System.out.println( GsmStatus.NOT_OPTIONABLE+ "=>> NOT_OPTIONABLE");
       System.out.println( GsmStatus.QUALITY_OF_SERVICE_NA+ "=>> QUALITY_OF_SERVICE_NA");
       System.out.println( GsmStatus.NO_INTERWORKING_AVAILABLE+ "=>> NO_INTERWORKING_AVAILABLE");
       System.out.println( GsmStatus.SMS_VALIDITY_EXPIRED+ "=>> SMS_VALIDITY_EXPIRED");
       System.out.println( GsmStatus.SMS_DELETED_BY_ORIGINATOR+ "=>> SMS_DELETED_BY_ORIGINATOR");
       System.out.println( GsmStatus.SMS_DELETED_BY_SMSC_ADMIN+ "=>> SMS_DELETED_BY_SMSC_ADMIN");
       System.out.println( GsmStatus.SMS_NOT_EXISTS+ "=>> SMS_NOT_EXISTS");
       System.out.println( GsmStatus.UNKNOWN_ERROR+ "=>> UNKNOWN_ERROR");

       System.out.println("Delivery report for SMPP constants");
       System.out.println(SmppStatus.STATE_ACCEPTED+ "=>> STATE_ACCEPTED");
       System.out.println(SmppStatus.STATE_DELETED+ "=>> STATE_DELETED");
       System.out.println(SmppStatus.STATE_DELIVERED+ "=>> STATE_DELIVERED");
       System.out.println(SmppStatus.STATE_ENROUTE+ "=>> STATE_ENROUTE");
       System.out.println(SmppStatus.STATE_EXPIRED+ "=>> STATE_EXPIRED");
       System.out.println(SmppStatus.STATE_REJECTED+ "=>> STATE_REJECTED");
       System.out.println(SmppStatus.STATE_SCHEDULED+ "=>> STATE_SCHEDULED");
       System.out.println(SmppStatus.STATE_SKIPPED+ "=>> STATE_SKIPPED");
       System.out.println(SmppStatus.STATE_UNDELIVERABLE+ "=>> STATE_UNDELIVERABLE");
       System.out.println(SmppStatus.STATE_UNKNOWN+ "=>> STATE_UNKNOWN");
    }

}
