package com.yas.webhook.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

class HmacUtilsTest {

    @Test
    void hash_whenCalled_thenReturnsDeterministicValue() throws NoSuchAlgorithmException, InvalidKeyException {
        String first = HmacUtils.hash("payload", "secret");
        String second = HmacUtils.hash("payload", "secret");

        assertNotNull(first);
        assertEquals(first, second);
        assertNotEquals(first, HmacUtils.hash("payload-2", "secret"));
    }
}