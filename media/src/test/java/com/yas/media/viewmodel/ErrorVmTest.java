package com.yas.media.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void convenienceConstructor_initializesEmptyFieldErrors() {
        ErrorVm error = new ErrorVm("400", "Bad request", "Invalid input");

        assertEquals("400", error.statusCode());
        assertEquals("Bad request", error.title());
        assertEquals("Invalid input", error.detail());
        assertTrue(error.fieldErrors().isEmpty());
    }

    @Test
    void canonicalConstructor_preservesFieldErrors() {
        List<String> fieldErrors = List.of("name is required");

        ErrorVm error = new ErrorVm("400", "Bad request", "Invalid input", fieldErrors);

        assertEquals(fieldErrors, error.fieldErrors());
    }
}
