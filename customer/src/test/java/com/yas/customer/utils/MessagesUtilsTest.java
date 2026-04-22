package com.yas.customer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenCodeIsValid_thenFormatsMessage() {
        String result = MessagesUtils.getMessage("WRONG_EMAIL_FORMAT", "World");

        assertEquals("Wrong email format for World", result);
    }

    @Test
    void getMessage_whenCodeIsInvalid_thenReturnsCode() {
        String result = MessagesUtils.getMessage("invalid.code");

        assertEquals("invalid.code", result);
    }

    @Test
    void getMessage_whenKeyMissing_thenFormatsFallbackMessage() {
        assertEquals("missing value", MessagesUtils.getMessage("missing {}", "value"));
    }
}