package com.objectxp.msg;


public class StatusReportMsg  extends StatusReportMessage{
    public StatusReportMsg() throws IllegalArgumentException {
        super();
        A(new SmppStatus(SmppStatus.STATE_DELIVERED, 0, 0));
        setType(3);
    }
}
