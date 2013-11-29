package ru.elmsoft.sms.transport;

import java.util.Properties;

import ru.elmsoft.sms.object.*;

import com.objectxp.msg.MessageException;

/**
 * ��������� ��� �������� � ������ ���������
 */
public interface SMSTransport {
    /**
     * ���������� ��������� � DLR
     *
     * @param listener ����������� ���������
     */
    void addDeliveryReportListener(DeliveryReportListener listener);

    /**
     * ���������� ��������� � �������� ����������
     *
     * @param listener ����������� ���������
     */
    void addIncomeSMSListener(IncomeSMSListener listener);

    /**
     * ���������� ��������� ��������� �������
     *
     * @param listener - ����������� ��������� ���������� �������
     */

    void addSystemReportListener(SystemReportListener listener);

    /**
     * �������� ���������
     *
     * @param message - ���������
     * @throws MessageException - � ������ ���� �������� ��������� �� �������� ��-�� ���� ���
     *                          ��������� �� ��������������� ��� �������� ������������ ���� �� ����������� ��������.
     */
    void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException;

    /**
     * ������������� ����������
     *
     * @param props - ��������� �������������
     * @throws MessageException - � ������ ���� ��������� ������ ������������� �� �������
     *                          ������������� �������� ��������� � ������������
     */
    void init(Properties props) throws MessageException;


    /**
     * ��������� ����������
     */
    void stop();


}