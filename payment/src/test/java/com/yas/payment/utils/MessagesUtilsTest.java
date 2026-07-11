package com.yas.payment.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenKeyIsMissing_thenFormatsTheProvidedCode() {
        assertEquals("missing value", MessagesUtils.getMessage("missing {}", "value"));
    }
}
