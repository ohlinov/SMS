package ru.elmsoft.sms.transport;

import java.util.Properties;

import ru.elmsoft.sms.object.*;

import com.objectxp.msg.MessageException;

/**
 * Транспорт для отправки и приема сообщения
 */
public interface SMSTransport {
    /**
     * Добавление слушателя о DLR
     *
     * @param listener добавляемый слушатель
     */
    void addDeliveryReportListener(DeliveryReportListener listener);

    /**
     * добавление слушателя о входящих сообщениях
     *
     * @param listener добавляемый слушатель
     */
    void addIncomeSMSListener(IncomeSMSListener listener);

    /**
     * Добавление слушателя системных событий
     *
     * @param listener - добавляемый слушатель системного события
     */

    void addSystemReportListener(SystemReportListener listener);

    /**
     * Отправка сообщения
     *
     * @param message - сообщение
     * @throws MessageException - в случае если отправка сообщения не возможна из-за того что
     *                          транспорт не инициализирован или перестал существовать один из необходимых ресурсов.
     */
    void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException;

    /**
     * Инициализация транспорта
     *
     * @param props - параметры инициализации
     * @throws MessageException - в случае если произошли ошибки инициализации по причине
     *                          недоступности ресурсов описанных в конфигурации
     */
    void init(Properties props) throws MessageException;


    /**
     * остановка транспорта
     */
    void stop();


}