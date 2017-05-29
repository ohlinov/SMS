package ru.elmsoft.sms.object;

import com.objectxp.msg.MessageEvent;

/**
 * 
 * Системное сообщение
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
     * Код события
     * 
     * @return Returns the code.
     */
    public int getCode() {
        return code;
    }

/**
 * 
 * @return текстовое описание событие
 */
    public String getDescription() {
        return description;
    }
/**
 * Возвращает тип события: 0 - носит информационный характер. 
 * 1 - сигнализирует о удачном завершении операции
 * -1 - сигнализирует об аварийной ситуации
 * @return вид события
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
 * @return событие библиотеки, которое породило данное событие
 */
    public MessageEvent getEvent() {
        return this.event;
    }
    
    public String toString(){
		return "System report:" + description +  " TYPE " + status + " 0-INFO, -1 - ERROR, 1 - SUCCESS";
	}

}
