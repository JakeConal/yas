package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_withUnknownCode_shouldReturnCodeAsMessage() {
        // When a message code is not found in the bundle, it returns the code itself
        String result = MessagesUtils.getMessage("non.existent.code");
        assertEquals("non.existent.code", result);
    }

    @Test
    void getMessage_withUnknownCodeAndVarArgs_shouldReturnCodeAsMessage() {
        String result = MessagesUtils.getMessage("non.existent.code", "arg1", "arg2");
        assertNotNull(result);
        // Falls back to the error code as the message
        assertEquals("non.existent.code", result);
    }
}
