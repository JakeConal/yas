package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_formatsKnownMessageKey() {
        assertThat(MessagesUtils.getMessage("PRODUCT_NOT_FOUND", 10L))
            .isEqualTo("Product 10 is not found");
    }

    @Test
    void getMessage_returnsKeyWhenMessageIsMissing() {
        assertThat(MessagesUtils.getMessage("UNKNOWN_MESSAGE_KEY"))
            .isEqualTo("UNKNOWN_MESSAGE_KEY");
    }
}