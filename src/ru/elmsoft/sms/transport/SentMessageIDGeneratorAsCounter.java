package ru.elmsoft.sms.transport;


/**
 * Генерирует последовательные id сообщения
 */
public class SentMessageIDGeneratorAsCounter implements SentMessageIdGenerator {
    /** The counter. */
    private long count = 0;

    public synchronized String generate() {
        if (count == Long.MAX_VALUE) {
            count = 0;
        }
        count++;
        return Long.toString(count);
    }
}
