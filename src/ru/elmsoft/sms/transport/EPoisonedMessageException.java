package ru.elmsoft.sms.transport;

public class EPoisonedMessageException extends Throwable {

  public EPoisonedMessageException() {
    super();
    
  }

  public EPoisonedMessageException(String message) {
    super(message);
   
  }

}
