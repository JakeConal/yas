package com.yas.product.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriceValidatorTest {

    private final PriceValidator priceValidator = new PriceValidator();

    @Test
    void isValid_returnsTrueForNonNegativeValues() {
        assertTrue(priceValidator.isValid(0.0, null));
        assertTrue(priceValidator.isValid(19.99, null));
    }

    @Test
    void isValid_returnsFalseForNegativeValues() {
        assertFalse(priceValidator.isValid(-0.01, null));
    }
}