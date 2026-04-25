package com.yas.customer.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    @Test
    void handleTypedFallback_whenCalled_thenRethrowsThrowable() {
        TestFallbackHandler fallbackHandler = new TestFallbackHandler();
        IllegalStateException throwable = new IllegalStateException("boom");

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> fallbackHandler.handleTypedFallback(throwable));

        assertSame(throwable, result);
    }

    @Test
    void handleBodilessFallback_whenCalled_thenRethrowsThrowable() {
        TestFallbackHandler fallbackHandler = new TestFallbackHandler();
        IllegalStateException throwable = new IllegalStateException("boom");

        IllegalStateException result = assertThrows(IllegalStateException.class,
            () -> fallbackHandler.handleBodilessFallback(throwable));

        assertSame(throwable, result);
    }

    private static final class TestFallbackHandler extends AbstractCircuitBreakFallbackHandler {
    }
}