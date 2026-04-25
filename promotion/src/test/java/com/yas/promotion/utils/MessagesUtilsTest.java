package com.yas.promotion.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    private ResourceBundle originalBundle;

    @BeforeEach
    void setUp() {
        originalBundle = MessagesUtils.messageBundle;
        MessagesUtils.messageBundle = new TestMessageBundle();
    }

    @AfterEach
    void tearDown() {
        MessagesUtils.messageBundle = originalBundle;
    }

    @Test
    void getMessage_whenKeyExists_thenFormatsArguments() {
        assertEquals("Promotion code1 is not found", MessagesUtils.getMessage("PROMOTION_NOT_FOUND", "code1"));
    }

    @Test
    void getMessage_whenKeyMissing_thenReturnsKey() {
        assertEquals("UNKNOWN_CODE", MessagesUtils.getMessage("UNKNOWN_CODE"));
    }

    private static class TestMessageBundle extends ListResourceBundle {

        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"PROMOTION_NOT_FOUND", "Promotion {} is not found"}
            };
        }
    }
}