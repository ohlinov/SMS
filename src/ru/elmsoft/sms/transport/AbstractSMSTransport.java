package ru.elmsoft.sms.transport;

import com.objectxp.msg.*;
import com.objectxp.msg.ems.EMSMessage;
import org.apache.log4j.Logger;
import ru.elmsoft.sms.object.*;
import ru.elmsoft.sms.transport.billing.file.BaseBillingFile;

import java.io.*;
import java.util.*;

/**
 * Данный класс реализует базовые алгоритмы работы с транспортами
 *
 * @author oleg
 */
public abstract class AbstractSMSTransport implements SMSTransport, MessageEventListener {

    private long delay;

    private boolean isRestart = false;

    private long connectTimeout;

    protected org.apache.log4j.Logger LOGGER ;

    private List/* MessageEvent */listener;

    private List/* DeliveryReportEvent */listener_report;

    private List /* SystemReportListener */listener_system;

    private Properties license ;

    protected Properties initParams;

    private Timer timer;

    private TimerTask isAliveTask;

    private Properties initProperies;

    private boolean isNotifySystemEvent;

    private boolean isExternalStop;

    private BaseBillingFile billing;

    protected abstract SmsService getService();

    private TimerTask createTimerTask() {
        getLogger().debug("Creating new timer....");
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
                // timer.schedule(isAliveTask, delay);
            }
        };
    }

    protected abstract void createService();

    protected abstract void dropService();

    public AbstractSMSTransport() {
        LOGGER = Logger.getLogger(getClass());
        LOGGER.warn("Called default constructor. Please call method init!!!");
    }

    /**
     * @param props параметры инициализации
     * @throws Exception -
     *                   если происходит ошибка инициализации, недоступен сервис или
     *                   любой другой ресурс описанный в настройках
     */
    public AbstractSMSTransport(Properties props) throws Exception {
        LOGGER = Logger.getLogger(getClass());
        try {
            // initProps = props;
            listener = Collections.synchronizedList(new LinkedList());
            listener_report = Collections.synchronizedList(new LinkedList());
            Collections.synchronizedList(new LinkedList());
            license = new Properties();
            initParams = new Properties();
            timer = new Timer(true);
            isAliveTask = createTimerTask();
            isNotifySystemEvent = true;
            init(props);
        } catch (MessageException err) {
            getLogger().error(err.getMessage(), err);
            throw new Exception(err);
        }
    }

    /**
     * Добавления слушателя о отчетах SMS
     */
    public synchronized void addDeliveryReportListener(DeliveryReportListener listener) {
        LOGGER.info("Added listener delivery reports");
        listener_report.add(listener);
    }

    /**
     * Добавление слушателя о входящих SMS
     */
    public synchronized void addIncomeSMSListener(IncomeSMSListener listener) {
        LOGGER.info("Added income message listener ");
        this.listener.add(listener);
    }

    /**
     * Добавление слушателя системных событий
     */
    public synchronized void addSystemReportListener(SystemReportListener listener) {
        LOGGER.info("Added system event listener");
        listener_system.add(listener);
    }

    /**
     * Инициализация транспорта. Данный метод необходимо вызвать, если для
     * создания транспорта использовался конструктор по умолчанию
     *
     * @param props параметры инициализации
     * @throws MessageException -  если происходит ошибка инициализации, недоступен сервис или
     *                   любой другой ресурс описанный в настройках
     */
    public synchronized void init(Properties props) throws MessageException {
        try {
            isNotifySystemEvent = true;
            initProperies = props;
            getLogger().info("Init service");
            File licenseFile = new File("license.txt");
            LOGGER.info("Loading file with license " + licenseFile.getAbsolutePath());
            license.load(new FileInputStream(licenseFile));
            writeToLog(license);
            getLogger().info("Init service properties");
            writeToLog(props);

            getLogger().debug("Property billing.file value is =>" + props.getProperty("billing.file"));
            if (props.getProperty("sms.manual.keepalive.interval") != null) {
                delay = Long.parseLong(props.getProperty("sms.manual.keepalive.interval"));
            } else {
                LOGGER.warn("sms.manual.keepalive.interval not setted in props. This parameter set to default value 0 (no timers)");
                delay = 0;
            }
            isRestart = false;
            if (props.getProperty("sms.manual.keepalive.restart") != null) {
                isRestart = props.getProperty("sms.manual.keepalive.restart").equals("yes");
            }
            if (props.getProperty("smpp.smpp.connector.timeout") != null) {
                connectTimeout = Long.parseLong(props.getProperty("smpp.smpp.connector.timeout"));
            } else {
                LOGGER.warn("smpp.smpp.connector.timeout not setted in props. This parameter set to default value 120 seconds");
                connectTimeout = 120000L;
            }
            if (props.getProperty("billing.file") != null) {
                LOGGER.info("Add billing listener");
                if (billing == null) {
                    billing = new BaseBillingFile();
                    addDeliveryReportListener(billing);
                    addIncomeSMSListener(billing);
                    addSystemReportListener(billing);
                    LOGGER.info("Billing listener added");
                } else {
                    LOGGER.info("Billing listener created later and don't need add that another");
                }
            } else {
                LOGGER.warn("Billing not configured");
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("License file not found", e);
            throw new MessageException(e.getMessage());

        } catch (IOException e) {
            LOGGER.error("Error on load license", e);
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
        LOGGER.info("Process init params");
        writeToLog(props);
        initParams.clear();
        LOGGER.info("Process init params only with this prefix => " + prefix);
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = (String) keys.nextElement();
            if (sKey.startsWith(prefix))
                initParams.put(sKey.substring(prefix.length()), props.get(sKey));
        }
        LOGGER.info("Choosing init params");
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
            LOGGER.info(key + "=" + (String) props.getProperty(key));
        }
    }

    /**
     * Вывод в лог параметров SMS
     *
     * @param message сообщение
     * @param comment комментарий
     */
    protected void logMessage(SMSMessage message, String comment) {
        LOGGER.info(comment);
        LOGGER.info("MessageID=>" + message.getMessageID());
      //  LOGGER.info("UDH=>" + message.getUDH());
        // LOGGER.info("SMSC=>" + message.getSMSC());
        LOGGER.info("PhoneNumberFrom=>" + message.getPhoneNumberFrom());
        LOGGER.info("PhoneNumberTo=>" + message.getPhoneNumberTo());
        LOGGER.info("message=>" + message.getMessage());
        //LOGGER.info("Data=>" + message.getData());
        LOGGER.info("Expiry" + Long.toString(message.getExpiry()));
        if (message.getExpiryDate() == null)
            LOGGER.info("ExpiryDate is not assigned");
        else
            LOGGER.info("ExpiryDate is assigned.");
        LOGGER.info(message.getSendedID());
    }

    /**
     * Извещение слушателей входящих сообщений о пришедшем сообщении
     *
     * @param message SMS сообщение
     */
    protected void notifyAllIncomeMessageListener(SMSMessage message) {
        getLogger().debug("Income message " + message.toString());
        Iterator iter = listener.iterator();
        while (iter.hasNext()) {
            IncomeSMSListener element = (IncomeSMSListener) iter.next();
            getLogger().debug("Sent event to  " + element);
            element.receiveIncomeSMS(message);
        }
    }

    /**
     * Извещение слушателей отчетов о сообщениях о пришедшем отчете
     *
     * @param report Delivery report
     */
    protected void notifyAllDLRListener(DeliveryReport report) {
        getLogger().debug("notifyAllDLRListener report" + report.toString());
        Iterator iter = listener_report.iterator();
        while (iter.hasNext()) {
            DeliveryReportListener element = (DeliveryReportListener) iter.next();
            getLogger().debug("Sent event to " + element.toString());
            element.receiveDeliveryReport(report);
        }
    }

    /**
     * Извещение слушателей о системных событиях
     *
     * @param message Delivery report
     */
    private void notifyAllSystemEventListener(SystemReport message) {
        if (!isNotifySystemEvent) {
            getLogger().debug("System event report blocked");
            return;
        }
        getLogger().debug("System report notify" + message.toString());

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
        getLogger().debug("System event=>" + (event != null ? event.toString() : "null"));
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
        LOGGER.info("Delivery Report");
        LOGGER.info(msg.getMessage());
        LOGGER.info("Name Mask=>" + key);
        LOGGER.info("State=>" + key);
        if (initParams.containsKey(key)) {
            if (!initParams.get(key).equals("true")) {
                return;
            }
        }
        report.setStateNumeric(code);
        String messageContent = msg.getUserData() != null ? new String (msg.getUserData()) : "";
        report.setStateString(msg.getMessage() + " " + messageContent);
        notifyAllDLRListener(report);
    }

    /**
     * остановка транспорта
     */
    private synchronized void stopService() {
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
     * @throws MessageException если при перезапуск не состоялся
     */
    public void restart() throws MessageException {
        // synchronized (LOGGER) {
        getLogger().info("Restart service");

        if (getService() == null)
            throw new MessageException("Service is not initialized");

        stopService();

        getLogger().info("wait " + connectTimeout + "ms");
        try {
            Thread.sleep(connectTimeout);
        } catch (InterruptedException err1) {
            err1.printStackTrace();
            getLogger().debug(null, err1);
        }


        try {
            if (!isExternalStop) {
                init(initProperies);
            } else {
                LOGGER.debug("ExternalStop flag is true. Skip start part of restart process.");
            }
        } catch (IllegalStateException err) {
            LOGGER.error("Error on restarting ", err);
            throw new MessageException(err.getMessage());
        } /*catch (IOException err) {
            LOGGER.error("Error on restarting", err);
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

        if (getService() == null) {
            throw new MessageException("Service is not initialized");
        }

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
                getLogger().error("Error on restarting timer.", err);
            }

        } catch (java.io.IOException e) {
            LOGGER.error("Error on init transport", e);
            throw new MessageException(e.getMessage());
        } catch (java.lang.Exception e) {
            LOGGER.error("Error on init transport", e);
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
            case MessageEvent.MESSAGE_RECEIVED:
                LOGGER.info("Message received");
                Message msg = event.getMessage();
                if (msg instanceof EMSMessage) {
                    LOGGER.info("EMS message");
                }
                processMessageReceived(event);
                restartTimer();
                break;

            case MessageEvent.MESSAGE_NOT_SENT:
                processSystemEvent(event, SystemReport.ERROR, "MESSAGE_NOT_SENT");
                onMessageSentEvent(event, false);
                getLogger().debug("Generate ERROR system event");
                LOGGER.warn("Message not sent");
                break;

            case MessageEvent.MESSAGE_SENT:
                LOGGER.info("Event Type: Message Sent");
                getLogger().debug("Generate SUCCESS system event");
                onMessageSentEvent(event, true);
                processSystemEvent(event, SystemReport.SUCCESS, "Message Sent");
                restartTimer();
                break;

            case MessageEvent.MULTIPART_FAILURE:
                LOGGER.info("Event Type: not all parts of a MultipartMessage received within a certain time");
                processSystemEvent(event, SystemReport.ERROR, "not all parts of a MultipartMessage received within a certain time");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.NETWORK_DISCONNECTED:
                LOGGER.info("Event Type: Network registration not ready.");
                processSystemEvent(event, SystemReport.ERROR, "Network registration not ready.");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.RECEIVING_STARTED:
                LOGGER.info("Event Type: Receiving started");
                processSystemEvent(event, SystemReport.SUCCESS, "Receiving started");
                getLogger().debug("Generate SUCCESS system event");
                break;
            case MessageEvent.RECEIVING_STOPPED:
                LOGGER.info("Event Type: Receiving stopped");
                processSystemEvent(event, SystemReport.ERROR, "Receiving stopped");
                getLogger().debug("Generate ERROR system event");
                break;
            case MessageEvent.STATUS_RECEIVED:
                LOGGER.info("Event type: status report received");
                processStatusReport(event);
                restartTimer();
                break;
            default:
                LOGGER.info(event.toString());
        }
        // }
    }

    protected void onMessageSentEvent(MessageEvent event, boolean isSent) {
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
            if (delay != 0) {
                isAliveTask = createTimerTask();
                timer.schedule(isAliveTask, delay, delay);
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

    private void setAdditionalParams(SMSMessage message, SmsMessage msg) {
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
            LOGGER.info("Warning!!! Cant receive delivery report ");
        }

    }

    protected void finalize() throws Throwable {
        stop();
        dropService();
        super.finalize();
    }

    protected org.apache.log4j.Logger getLogger() {
        return LOGGER;
    }

    protected void setExternalStopFlag(boolean value) {
        getLogger().debug("Set extrernal stop flag to value => " + value);
        isExternalStop = value;
    }
}
