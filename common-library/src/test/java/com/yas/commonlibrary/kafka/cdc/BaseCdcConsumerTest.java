package com.yas.commonlibrary.kafka.cdc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;

class BaseCdcConsumerTest {

    // Concrete subclass for testing the abstract class
    static class TestCdcConsumer extends BaseCdcConsumer<String, String> {
        public void consumeSingle(String record, MessageHeaders headers, Consumer<String> consumer) {
            processMessage(record, headers, consumer);
        }

        public void consumeKeyValue(String key, String value, MessageHeaders headers,
                                    BiConsumer<String, String> consumer) {
            processMessage(key, value, headers, consumer);
        }
    }

    private TestCdcConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TestCdcConsumer();
    }

    private MessageHeaders buildHeaders(String key) {
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put(KafkaHeaders.RECEIVED_KEY, key);
        return new MessageHeaders(headerMap);
    }

    @Test
    @SuppressWarnings("unchecked")
    void processMessage_singleRecord_shouldInvokeConsumer() {
        Consumer<String> singleConsumer = mock(Consumer.class);
        MessageHeaders headers = buildHeaders("key-1");

        consumer.consumeSingle("payload", headers, singleConsumer);

        verify(singleConsumer).accept("payload");
    }

    @Test
    @SuppressWarnings("unchecked")
    void processMessage_keyValueRecord_shouldInvokeBiConsumer() {
        BiConsumer<String, String> biConsumer = mock(BiConsumer.class);
        MessageHeaders headers = buildHeaders("key-2");

        consumer.consumeKeyValue("key-2", "value-2", headers, biConsumer);

        verify(biConsumer).accept("key-2", "value-2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void processMessage_withNullKey_shouldInvokeConsumer() {
        Consumer<String> singleConsumer = mock(Consumer.class);
        MessageHeaders headers = buildHeaders(null);

        consumer.consumeSingle("payload", headers, singleConsumer);

        verify(singleConsumer).accept("payload");
    }
}
