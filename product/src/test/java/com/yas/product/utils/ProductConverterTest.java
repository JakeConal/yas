package com.yas.product.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductConverterTest {

    @Test
    void toSlug_normalizesWhitespaceAndPunctuation() {
        assertThat(ProductConverter.toSlug("  Hello, World!  "))
            .isEqualTo("hello-world");
    }
}