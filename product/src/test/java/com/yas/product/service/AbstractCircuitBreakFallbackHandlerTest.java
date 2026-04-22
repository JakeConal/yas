package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private static class TestFallbackHandler extends AbstractCircuitBreakFallbackHandler {
        void callBodilessFallback(Throwable throwable) throws Throwable {
            handleBodilessFallback(throwable);
        }

        <T> T callTypedFallback(Throwable throwable) throws Throwable {
            return handleTypedFallback(throwable);
        }
    }

    private final TestFallbackHandler handler = new TestFallbackHandler();

    @Test
    void handleTypedFallback_rethrowsThrowable() {
        IllegalStateException throwable = new IllegalStateException("boom");

        assertThrows(IllegalStateException.class, () -> handler.callTypedFallback(throwable));
    }

    @Test
    void handleBodilessFallback_rethrowsThrowable() {
        IllegalStateException throwable = new IllegalStateException("boom");

        assertThrows(IllegalStateException.class, () -> handler.callBodilessFallback(throwable));
    }
}