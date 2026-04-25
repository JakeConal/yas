package com.yas.customer.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LocationServiceFallbackTest {

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(org.mockito.Mockito.mock(RestClient.class),
            org.mockito.Mockito.mock(com.yas.customer.config.ServiceUrlConfig.class));
    }

    @Test
    void handleAddressDetailListFallback_whenCalled_thenRethrowsThrowable() throws Exception {
        assertFallbackRethrows("handleAddressDetailListFallback");
    }

    @Test
    void handleAddressDetailFallback_whenCalled_thenRethrowsThrowable() throws Exception {
        assertFallbackRethrows("handleAddressDetailFallback");
    }

    @Test
    void handleAddressFallback_whenCalled_thenRethrowsThrowable() throws Exception {
        assertFallbackRethrows("handleAddressFallback");
    }

    private void assertFallbackRethrows(String methodName) throws Exception {
        Method method = LocationService.class.getDeclaredMethod(methodName, Throwable.class);
        method.setAccessible(true);
        IllegalStateException throwable = new IllegalStateException("boom");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
            () -> method.invoke(locationService, throwable));

        assertSame(throwable, exception.getCause());
    }
}