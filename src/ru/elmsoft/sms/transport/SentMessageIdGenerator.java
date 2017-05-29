package ru.elmsoft.sms.transport;

/**
 * Генератор идентификаторов отправленных сообщений
 */
public interface SentMessageIdGenerator {
    /**
     * Генерирует ид сообщения
     * @return уникальный ид сообщения
     */
    String generate();
}
