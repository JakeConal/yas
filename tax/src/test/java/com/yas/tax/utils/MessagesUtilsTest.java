package com.yas.tax.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_whenBundleDoesNotContainKey_thenFormatsOriginalCode() {
        assertEquals("missing value", MessagesUtils.getMessage("missing {}", "value"));
    }
}