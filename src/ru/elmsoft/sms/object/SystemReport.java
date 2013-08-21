package ru.elmsoft.sms.object;

import com.objectxp.msg.MessageEvent;

/**
 * 
 * ��������� ���������
 *
 */
public class SystemReport {
    private int code;

    private int status = 0;

    private String description;

    public static int INFO = 0;

    public static int SUCCESS = 1;

    public static int ERROR = -1;

    private MessageEvent event;
    /**
     * ���-�������
     * 
     * @return Returns the code.
     */
    public int getCode() {
        return code;
    }

/**
 * 
 * @return ��������� �������� �������
 */
    public String getDescription() {
        return description;
    }
/**
 * ���������� ��� �������: 0 - ����� �������������� ��������. 
 * 1 - ������������� � ������� ���������� ��������
 * -1 - ������������� �� ��������� ��������
 * @return ��� �������
 */
    public int getStatus() {
        return this.status;
    }

    public SystemReport(int code, int status, String description, MessageEvent event) {
        this.code = code;
        this.description = description;
        this.status = status;
        this.event = event;
    }
/**
 * 
 * @return ������� ����������, ������� �������� ������ �������
 */
    public MessageEvent getEvent() {
        return this.event;
    }
    
    public String toString(){
		return "System report:" + description +  " TYPE " + status + " 0-INFO, -1 - ERROR, 1 - SUCCESS";
	}

}
