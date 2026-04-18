package com.yas.commonlibrary.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    @Test
    void format_withDefaultPattern_shouldReturnExpectedString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
        String result = DateTimeUtils.format(dateTime);
        assertEquals("15-06-2024_10-30-45", result);
    }

    @Test
    void format_withCustomPattern_shouldReturnExpectedString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        String result = DateTimeUtils.format(dateTime, "yyyy/MM/dd");
        assertEquals("2024/01/01", result);
    }

    @Test
    void format_withDefaultPattern_shouldNotBeNull() {
        String result = DateTimeUtils.format(LocalDateTime.now());
        assertNotNull(result);
        assertTrue(result.contains("_"));
    }
}
