package com.yas.tax.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    @Test
    void handleTypedFallback_whenCalled_thenReThrowsThrowable() {
        TestFallbackHandler fallbackHandler = new TestFallbackHandler();
        IllegalStateException exception = new IllegalStateException("boom");

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> fallbackHandler.handleTypedFallback(exception));

        assertSame(exception, result);
    }

    @Test
    void handleBodilessFallback_whenCalled_thenReThrowsThrowable() {
        TestFallbackHandler fallbackHandler = new TestFallbackHandler();
        IllegalStateException exception = new IllegalStateException("boom");

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> fallbackHandler.handleBodilessFallback(exception));

        assertSame(exception, result);
    }

    private static final class TestFallbackHandler extends AbstractCircuitBreakFallbackHandler {
    }
}