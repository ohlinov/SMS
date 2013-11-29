package ru.elmsoft.sms.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ru.elmsoft.sms.object.DeliveryReport;
import ru.elmsoft.sms.object.DeliveryReportListener;
import ru.elmsoft.sms.object.IncomeSMSListener;
import ru.elmsoft.sms.object.SMSMessage;
import ru.elmsoft.sms.object.SystemReport;
import ru.elmsoft.sms.object.SystemReportListener;
import ru.elmsoft.sms.transport.billing.file.BaseBillingFile;

import com.objectxp.msg.GsmHelper;
import com.objectxp.msg.Message;
import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageEventListener;
import com.objectxp.msg.MessageException;
import com.objectxp.msg.SmsMessage;
import com.objectxp.msg.SmsService;
import com.objectxp.msg.StatusReportMessage;
import com.objectxp.msg.ems.EMSMessage;

/**
 * Данный класс реализует базовые алгоритмы работы с транспортами
 *
 * @author oleg
 */
public abstract class AbstractSMSTransport implements SMSTransport, MessageEventListener {

    private long DELAY = 0;

    private boolean isRestart = false;

    private long connect_timeout = 120100;

    protected static org.apache.log4j.Logger logger;

    protected List/* MessageEvent */listener = Collections.synchronizedList(new LinkedList());

    protected List/* DeliveryReportEvent */listener_report = Collections.synchronizedList(new LinkedList());

    protected List /* SystemReportListener */listener_system = Collections.synchronizedList(new LinkedList());

    protected Properties license = new Properties();

    protected Properties initParams = new Properties();

    private Timer timer = new Timer(true);

    private TimerTask isAliveTask = createTimerTask();

    private Properties initProperies;

    private boolean isNotifySystemEvent = true;

    private boolean isExternalStop;

    private BaseBillingFile billing;

    protected abstract SmsService getService();

    private TimerTask createTimerTask() {
        Logger.getLogger(AbstractSMSTransport.class).debug("Creating new timer....");
        return new TimerTask() {
            public void run() {

                try {
                    isNotifySystemEvent = false;
                    getLogger().debug("Running timer task");
                    if (!getService().isAlive()) {
                        getLogger().error("Service is not alive...");
                        if (isRestart && !isExternalStop) {
                            restart();
                        } else {
                            isNotifySystemEvent = true;
                            processSystemEvent(null, SystemReport.ERROR, "DEVICE_NOT_RESPONDING");
                        }
                    }
                } catch (Exception err) {
                    getLogger().error(err);
                    isNotifySystemEvent = true;
                    err.printStackTrace();
                    getLogger().debug(null, err);
                    getLogger().debug("Send system event");
                    processSystemEvent(null, SystemReport.ERROR, "DEVICE_NOT_RESPONDING");
                } finally {
                    isNotifySystemEvent = true;
                }
                // timer.schedule(isAliveTask, DELAY);
            }
        };
    }

    protected abstract void createService();

    protected abstract void dropService();

    public AbstractSMSTransport() {
        super();
        logger = Logger.getLogger(getClass());
        logger.warn("Called default constructor. Please call method init!!!");
    }

    /**
     * @param props параметры инициализации
     * @throws Exception -
     *                   если происходит ошибка инициализации, недоступен сервис или
     *                   любой другой ресурс описанный в настройках
     */
    public AbstractSMSTransport(Properties props) throws Exception {
        super();
        logger = Logger.getLogger(getClass());
        try {
            // initProps = props;
            init(props);
        } catch (MessageException err) {
            logger.error(err);

            err.printStackTrace();
            getLogger().debug(null, err);
            throw new Exception(err);
        }
    }

    /**
     * Добавления слушателя о отчетах SMS
     */
    public void addDeliveryReportListener(DeliveryReportListener listener) {
        logger.info("Added listener delivery reports");
        synchronized (listener) {
            listener_report.add(listener);
        }

    }

    /**
     * Добавление слушателя о входящих SMS
     */
    public void addIncomeSMSListener(IncomeSMSListener listener) {
        logger.info("Added income message listener ");
        synchronized (listener) {
            this.listener.add(listener);
        }

    }

    /**
     * Добавление слушателя системных событий
     */
    public void addSystemReportListener(SystemReportListener listener) {
        logger.info("Added system event listener");
        synchronized (listener_system) {
            listener_system.add(listener);
        }

    }

    /**
     * Инициализация транспорта. Данный метод необходимо вызвать, если для
     * создания транспорта использовался конструктор по умолчанию
     *
     * @param props параметры инициализации
     * @throws Exception -
     *                   если происходит ошибка инициализации, недоступен сервис или
     *                   любой другой ресурс описанный в настройках
     */
    public void init(Properties props) throws MessageException {
        // synchronized (getService()) {
        try {
            isNotifySystemEvent = true;
            initProperies = props;
            getLogger().info("Init service");
            logger.info("Loading file with license");
            license.load(new FileInputStream(new File("license.txt")));
            writeToLog(license);
            getLogger().info("Init service propertyes");
            writeToLog(props);

            getLogger().debug("Property billing.file value is =>" + props.getProperty("billing.file"));
            if (props.getProperty("sms.manual.keepalive.interval") != null) {
                DELAY = Long.parseLong(props.getProperty("sms.manual.keepalive.interval"));
            } else {
                logger.warn("sms.manual.keepalive.interval not setted in props. This parameter set to default value 0 (no timers)");
            }
            isRestart = false;
            if (props.getProperty("sms.manual.keepalive.restart") != null) {
                isRestart = props.getProperty("sms.manual.keepalive.restart").equals("yes");
            }
            if (props.getProperty("smpp.smpp.connector.timeout") != null) {
                connect_timeout = Long.parseLong(props.getProperty("smpp.smpp.connector.timeout"));
            } else {
                logger.warn("smpp.smpp.connector.timeout not setted in props. This parameter set to default value 320000 ms");
            }
            if (props.getProperty("billing.file") != null) {
                logger.info("Add billing listener");
                if (billing == null) {

                    billing = new BaseBillingFile(/* props.getProperty("billing.file") */);
                    addDeliveryReportListener(billing);
                    addIncomeSMSListener(billing);
                    addSystemReportListener(billing);
                    logger.info("Billing listener added");
                } else {
                    logger.info("Billing listener created later and don't need add that another");
                }
            } else {
                logger.warn("Billing not configured");
            }


        } catch (FileNotFoundException e) {
            logger.error("License file not found", e);
            e.printStackTrace();
            getLogger().debug(null, e);
            throw new MessageException(e.getMessage());

        } catch (IOException e) {
            logger.error("Error on load license", e);
            e.printStackTrace();
            getLogger().debug(null, e);
            throw new MessageException(e.getMessage());
        }

    }

    /**
     * Обработка входящих параметров
     *
     * @param props  параметры из конфигурационного файла
     * @param prefix префикс. Для фильтрации параметров
     */
    protected void processInitParams(Properties props, String prefix) {
        logger.info("Process init params");
        writeToLog(props);
        initParams.clear();
        logger.info("Process init params only with this prefix => " + prefix);
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = (String) keys.nextElement();
            if (sKey.startsWith(prefix))
                initParams.put(sKey.substring(prefix.length()), props.get(sKey));
        }
        logger.info("Choosing init params");
        writeToLog(initParams);
    }

    /**
     * Вывод параметров конфигурации в файл
     *
     * @param props значения параметров конфигурации
     */
    private void writeToLog(Properties props) {
        Enumeration enums = props.keys();
        while (enums.hasMoreElements()) {
            String key = (String) enums.nextElement();
            logger.info(key + "=" + (String) props.getProperty(key));
        }
    }

    /**
     * Вывод в лог параметров SMS
     *
     * @param message сообщение
     * @param comment комментарий
     */
    protected void logMessage(SMSMessage message, String comment) {
        logger.info(comment);
        logger.info("MessageID=>" + message.getMessageID());
        logger.info("UDH=>" + message.getUDH());
        // logger.info("SMSC=>" + message.getSMSC());
        logger.info("PhoneNumberFrom=>" + message.getPhoneNumberFrom());
        logger.info("PhoneNumberTo=>" + message.getPhoneNumberTo());
        logger.info("Data=>" + message.getData());
        logger.info("Expiry" + Long.toString(message.getExpiry()));
        if (message.getExpiryDate() == null)
            logger.info("ExpiryDate is not assigned");
        else
            logger.info("ExpiryDate is assigned.");
        logger.info(message.getSendedID());
    }

    /**
     * Извещение слушателей входящих сообщений о пришедшем сообщении
     *
     * @param message SMS сообщение
     */
    protected void notifyAllIncomeMessageListener(SMSMessage message) {
        getLogger().debug("Income message " + message.toString());
        synchronized (listener) {
            Iterator iter = listener.iterator();
            while (iter.hasNext()) {
                IncomeSMSListener element = (IncomeSMSListener) iter.next();
                getLogger().debug("Sent event to  " + element);
                element.receiveIncomeSMS(message);
            }
        }
    }

    /**
     * Извещение слушателей отчетов о сообщениях о пришедшем отчете
     *
     * @param report Delivery report
     */
    protected void notifyAllDLRListener(DeliveryReport report) {
        getLogger().debug("notifyAllDLRListener report" + report.toString());
        synchronized (listener_report) {

            Iterator iter = listener_report.iterator();
            while (iter.hasNext()) {

                DeliveryReportListener element = (DeliveryReportListener) iter.next();
                getLogger().debug("Sent event to " + element.toString());
                element.receiveDeliveryReport(report);
            }
        }
    }

    /**
     * Извещение слушателей о системных событиях
     *
     * @param message Delivery report
     */
    protected void notifyAllSystemEventListener(SystemReport message) {
        if (!isNotifySystemEvent) {
            getLogger().debug("System event report blocked");
            return;
        }
        getLogger().debug("System report notify" + message.toString());


        synchronized (listener_system) {

            Iterator iter = listener_system.iterator();
            while (iter.hasNext()) {

                SystemReportListener element = (SystemReportListener) iter.next();
                getLogger().debug("Sent event to " + element.toString());
                try {
                    element.receiveSystemReport(message);
                } catch (Exception err) {
                    getLogger().error(err);
                    getLogger().debug(null, err);
                    err.printStackTrace();
                }
            }
        }
    }

    /**
     * Обработка события о входящем сообщении. В результате чего формируется
     * объект пригодный для отправки в адаптер
     *
     * @param event Описание входящего сообщения
     */
    protected void processMessageReceived(MessageEvent event) {

        SMSMessage message = new SMSMessage();
        message.setSendedID(event.getMessage().getID());
        // getLogger().debug("Message has data coding => " +
        // (SMSMessage)event.getMessage() )
        try {
            getLogger().debug("Dump message " + GsmHelper.encodeIA5(event.getMessage().getBytes()));
        } catch (NullPointerException err) {
            getLogger().debug("Message is empty !!!");
        }
        message.setMessage(event.getMessage().getMessage());
        message.setPhoneNumberFrom(event.getMessage().getSender());
        message.setPhoneNumberTo(event.getMessage().getRecipient());
        notifyAllIncomeMessageListener(message);
    }

    /**
     * Обработка системного события. Преобразование в системное событие для
     * объект, который может быть проанализирован адаптером
     *
     * @param event Описание входящего сообщения
     */
    protected void processSystemEvent(MessageEvent event, int status, String message) {
        SystemReport report = new SystemReport(event != null ? event.getType() : 2, status, message, event);

        notifyAllSystemEventListener(report);
        getLogger().debug("System event=>" + event != null ? event.toString() : "null");
    }

    /**
     * Обработка отчета о доставке (Delivery report). Преобразование в системное
     * событие для объект, который может быть проанализирован адаптером
     *
     * @param event Описание входящего сообщения
     */
    protected abstract void processStatusReport(MessageEvent event);

    /**
     * извещение о пришедших dlr. Отчет о доставке будет допущен слушателям если
     * он не запрещен по маске. Маска настровивается в конфигурационном файле
     *
     * @param msg    сообщение
     * @param report
     * @param key    -
     *               наименование маски
     * @param code   код сообщения
     */
    protected void notifyDeliveryReport(StatusReportMessage msg, DeliveryReport report, String key, int code) {
        logger.info("Delivery Report");
        logger.info(msg.getMessage());
        logger.info("Name Mask=>" + key);
        logger.info("State=>" + key);
        if (initParams.containsKey(key)) {
            if (!initParams.get(key).equals("true")) {
                return;
            }
        }
        report.setStateNumeric(code);
        report.setStateString(msg.getMessage());
        notifyAllDLRListener(report);

        return;
    }

    /**
     * остановка транспорта
     */
    protected synchronized void stopService() {
        getLogger().debug("Stop service");

        if (getService() == null) {
            getLogger().error("Service is not initialized");
            return;
        }
        synchronized (getService()) {

            try {
                // canChangeState = false;
                getService().stopReceiving();
                if (getService().isConnected()) {
                    if (!getService().isInitialized()) {
                        getLogger().debug("BIG PROBLEM!!!");

                        try {
                            Properties initProps = new Properties();
                            initProps.putAll(license);
                            initProps.putAll(initParams);
                            getService().init(initProps);
                        } catch (Exception err) {
                            getLogger().error(err);
                            getLogger().debug(null, err);
                            err.printStackTrace();
                        }
                    }
                    try {
                        getService().disconnect();
                    } catch (MessageException err) {
                        getLogger().error(err);
                        getLogger().debug(null, err);
                        err.printStackTrace();
                    }
                } else {
                    getLogger().warn("Service already stopped");
                }
            } catch (NullPointerException err) {
                getLogger().error(err);
                getLogger().debug(null, err);
                err.printStackTrace();
            } finally {
                getService().destroy();
                getLogger().info("Connection closed!!!");
                dropService();
                stopTimer();
                // canChangeState = true;
            }
        }
    }

    protected void stopTimer() {
        getLogger().info("stop timer");
        if (isAliveTask != null)
            isAliveTask.cancel();
        isAliveTask = null;
    }

    public synchronized void stop() {
        setExternalStopFlag(true);
        stopService();
    }

    /**
     * перезапуск транспорта
     *
     * @throws
     */
    public void restart() throws MessageException {
        // synchronized (logger) {
        getLogger().info("Restart service");

        if (getService() == null)
            throw new MessageException("Service is not initialized");

        stopService();

        getLogger().info("wait " + connect_timeout + "ms");
        try {
            Thread.sleep(connect_timeout);
        } catch (InterruptedException err1) {
            err1.printStackTrace();
            getLogger().debug(null, err1);
        }


        try {
            if (!isExternalStop) {
                init(initProperies);
            } else {
                logger.debug("ExternalStop flag is true. Skip start part of restart process.");
            }
        } catch (IllegalStateException err) {
            logger.error("Error on restarting ", err);
            err.printStackTrace();
            getLogger().debug(null, err);
            throw new MessageException(err.getMessage());
        } /*catch (IOException err) {
            logger.error("Error on restarting", err);
			err.printStackTrace();
			getLogger().debug(null, err);
			throw new MessageException(err.getMessage());
		}*/

    }

    /**
     * старт транспорта
     *
     * @throws MessageException в случае если транспорт не удается перевести в рабочее
     *                          стояние
     */
    protected void startService() throws MessageException {
        getLogger().debug("Start service");

        if (getService() == null)
            throw new MessageException("Service is not initialized");

        try {

            Properties initProps = new Properties();
            initProps.putAll(license);
            initProps.putAll(initParams);
            getService().init(initProps);

            subscribeListener();

            // getService().setKeepAliveInterval(0);
            getLogger().debug("Connecting....");
            // getService().isAlive();
            getLogger().debug("Passed props into service");
            writeToLog(getService().getProperties());
            getService().connect();
            getService().startReceiving();
            try {
                restartTimer();
            } catch (Exception err) {
                err.printStackTrace();
                getLogger().debug(null, err);
                getLogger().error("Error on restarting timer.");
            }

        } catch (java.io.IOException e) {
            logger.error("Error on init transport", e);
            e.printStackTrace();
            getLogger().debug(null, e);
            throw new MessageException(e.getMessage());
        } catch (java.lang.Exception e) {
            logger.error("Error on init transport", e);
            e.printStackTrace();
            getLogger().debug(null, e);
            throw new MessageException(e.getMessage());
        }

    }

    protected void subscribeListener() {
        getService().addMessageEventListener(this);
    }

    /**
     * Метод для приема системных событий от библиотеки jSMS
     */

    public void handleMessageEvent(MessageEvent event) {
        // synchronized (getService()) {
        // Thread.dumpStack();
        switch (event.getType()) {
            case MessageEvent.DEVICE_NOT_READY:
                processSystemEvent(event, SystemReport.ERROR, "DEVICE_NOT_READY");
                getLogger().debug("Generate Error system event");
                break;
            case MessageEvent.DEVICE_NOT_RESPONDING:
                processSystemEvent(event, SystemReport.ERROR, "DEVICE_NOT_RESPONDING");
                getLogger().debug("Device not response. Must be restart");
                getLogger().debug("Generate Error system event");
            /* restart(); */
                break;
            case MessageEvent.DEVICE_READY:
                processSystemEvent(event, SystemReport.SUCCESS, "DEVICE_READY");
                getLogger().debug("Generate SUCCESS system event");
                break;
            case MessageEvent.INCOMING_CALL:
                processSystemEvent(event, SystemReport.INFO, "INCOMING_CALL");
                getLogger().debug("Generate INFO system event");
                break;
            case MessageEvent.MESSAGE_NOT_SENT:
                processSystemEvent(event, SystemReport.ERROR, "MESSAGE_NOT_SENT");

                getLogger().debug("Generate ERROR system event");
                logger.warn("Message not sent");
                break;

            case MessageEvent.MESSAGE_RECEIVED:
                logger.info("Message received");
                Message msg = event.getMessage();
                if (msg instanceof EMSMessage) {
                    logger.info("EMS message");
                }
                processMessageReceived(event);
                restartTimer();
                /*******************************************************************
                 * DEBUG******* getLogger().debug(".TAG_SAR_TOTAL_SEGMENTS =>" +
                 * ((SmppMessage)event.getMessage()).getOptionalParameter(SmppOptionalParameter.TAG_SAR_TOTAL_SEGMENTS) );
                 * getLogger().debug(".TAG_SAR_SEGMENT_SEQNUM =>" +
                 * ((SmppMessage)event.getMessage()).getOptionalParameter(SmppOptionalParameter.TAG_SAR_SEGMENT_SEQNUM) );
                 * getLogger().debug(".TAG_SAR_MSG_REF_NUM =>" +
                 * ((SmppMessage)event.getMessage()).getOptionalParameter(SmppOptionalParameter.TAG_SAR_MSG_REF_NUM) );
                 * SmppMessage mesg = (SmppMessage)event.getMessage(); // if
                 * (mesg.containsUserDataHeader()) { try { SmsHeader header =
                 * SmsHeader.parseHeader(mesg.getUserDataHeader()); if
                 * (mesg.getUserDataHeader()!=null)
                 * getLogger().info(mesg.getUserDataHeader().length+"UDH length");
                 * else getLogger().debug("UDH is null"); Enumeration enumer =
                 * header.elements(); getLogger().debug("enumer =>" +
                 * enumer.hasMoreElements()); while (enumer.hasMoreElements()) {
                 * Object object = (Object) enumer.nextElement();
                 * getLogger().debug(object); } } catch (HeaderParseException err) {
                 *
                 * err.printStackTrace(); } //} DEBUG********
                 ******************************************************************/
                break;

            case MessageEvent.MESSAGE_SENT:
                logger.info("Event Type: Message Sent");
                processSystemEvent(event, SystemReport.SUCCESS, "Message Sent");
                getLogger().debug("Generate SUCCESS system event");

                restartTimer();
                break;

            case MessageEvent.MULTIPART_FAILURE:
                logger.info("Event Type: not all parts of a MultipartMessage received within a certain time");
                processSystemEvent(event, SystemReport.ERROR, "not all parts of a MultipartMessage received within a certain time");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.NETWORK_DISCONNECTED:
                logger.info("Event Type: Network registration not ready.");
                processSystemEvent(event, SystemReport.ERROR, "Network registration not ready.");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.RECEIVING_STARTED:
                logger.info("Event Type: Receiving started");
                processSystemEvent(event, SystemReport.SUCCESS, "Receiving started");
                getLogger().debug("Generate SUCCESS system event");
                break;
            case MessageEvent.RECEIVING_STOPPED:
                logger.info("Event Type: Receiving stopped");
                processSystemEvent(event, SystemReport.ERROR, "Receiving stopped");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.STATUS_RECEIVED:
                logger.info("Event type: status report received");
                processStatusReport(event);
                restartTimer();
                break;
            default:
                logger.info(event.toString());
        }
        // }
    }

    protected void restartTimer() {
        getLogger().debug("Restarting timer");
        try {
            if (isAliveTask != null) {
                isAliveTask.cancel();
                isAliveTask = null;
            }
        } catch (IllegalStateException err) {
            getLogger().error(err);
            err.printStackTrace();
            getLogger().debug(null, err);
        }
        try {
            if (DELAY != 0) {
                isAliveTask = createTimerTask();
                timer.schedule(isAliveTask, DELAY, DELAY);
            }
        } catch (IllegalStateException err) {
            getLogger().error(err);
            err.printStackTrace();
            getLogger().debug(null, err);
        }
    }

    protected void setAdressParams(SMSMessage message, SmsMessage msg) throws EPoisonedMessageException {
        if (message.getPhoneNumberTo() == null)
            throw new EPoisonedMessageException("message.getPhoneNumberTo() is null");
        msg.setRecipient(message.getPhoneNumberTo());
    }

    protected void setAdditionalParams(SMSMessage message, SmsMessage msg) {
        msg.requestStatusReport(true);
        if (message.getExpiryDate() == null)
            msg.setValidityPeriod(message.getExpiry());
        else
            msg.setValidityPeriod(message.getExpiryDate());
    }

    protected SmsMessage createMessage(SMSMessage message) {
        // проверим текст сообщения

        SmsMessage msg = new SmsMessage();

        getLogger().debug("Set msg id (setMessageId) =>" + message.getMessageID());
        msg.setID(message.getMessageID());
        if (!message.getMessage().matches("[\\p{ASCII}]*")) {
            getLogger().debug("Message  has unicode symbols");
            msg.setAlphabet(SmsMessage.DC_UCS2);
            if (message.getType() == SMSMessage.MESSAGE_TEXT)
                if (initParams.containsKey("message.encoding")) {
                    try {
                        // msg.setAlphabet(SmsMessage.DC_UCS2);
                        msg.setUserData(message.getMessage().getBytes(initParams.getProperty("message.encoding")));

                    } catch (UnsupportedEncodingException err) {
                        msg.setAlphabet(SmsMessage.DC_DEFAULT);
                        msg.setMessage(message.getMessage());
                        getLogger().debug("Encoding message not setted" + initParams.getProperty("message.encoding"));
                    }
                } else {
                    msg.setMessage(message.getMessage());
                }
        } else {
            getLogger().debug("Message  has only ascii symbols");
            msg.setCodingGroup(SmsMessage.DC_GROUP_DATA);
            msg.setAlphabet(SmsMessage.DC_DEFAULT);
            msg.setMessage(message.getMessage());
        }
        if (message.getUDH() != null) {
            if (message.getUDH().length > 0)
                msg.setUserDataHeader(message.getUDH());
        }
        return msg;
    }

    /**
     * Отсылка SMS сообщения.
     *
     * @param message SMS сообщение
     */
    public synchronized void sendSMSMessage(SMSMessage message) throws MessageException, EPoisonedMessageException {
        if (getService() == null) {
            throw new MessageException("Service is not initialized");
        }

        if (message == null) {
            throw new MessageException("message (input parameter) is null");
        }

        SmsMessage msg = createMessage(message);

        setAdressParams(message, msg);
        setAdditionalParams(message, msg);

        getService().sendMessage(msg);
        message.setSendedID(msg.getID());
        logMessage(message, "Message sent with  ID " + msg.getID());
        message.setSendedID(msg.getID());
        if (!msg.requestStatusReport()) {
            logger.info("Warning!!! Cant receive delivery report ");
        }

    }

    protected void finalize() throws Throwable {
        stop();
        dropService();
        super.finalize();
    }

    protected org.apache.log4j.Logger getLogger() {
        return logger;
    }

    protected void setExternalStopFlag(boolean value) {
        getLogger().debug("Set extrernal stop flag to value => " + value);
        isExternalStop = value;
    }
}
