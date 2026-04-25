package com.yas.promotion.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractCircuitBreakFallbackHandlerTest {

    private final TestFallbackHandler fallbackHandler = new TestFallbackHandler();

    @Test
    void handleFallback_whenThrowableIsThrown_thenRethrowsSameThrowable() {
        RuntimeException throwable = new RuntimeException("boom");

        Throwable exception = assertThrows(Throwable.class,
            () -> fallbackHandler.handleFallback(List.of(1L, 2L), throwable));

        assertSame(throwable, exception);
    }

    @Test
    void handleBodilessFallback_whenThrowableIsThrown_thenRethrowsSameThrowable() {
        IllegalStateException throwable = new IllegalStateException("boom");

        Throwable exception = assertThrows(Throwable.class,
            () -> fallbackHandler.handleBodilessFallback(List.of(1L), throwable));

        assertSame(throwable, exception);
    }

    private static class TestFallbackHandler extends AbstractCircuitBreakFallbackHandler {
    }
}