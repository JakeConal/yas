package com.yas.commonlibrary.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ExceptionTest {

    // ── AccessDeniedException ──────────────────────────────────────────────
    @Test
    void accessDeniedException_shouldStoreMessage() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");
        assertEquals("forbidden", ex.getMessage());
    }

    // ── BadRequestException ────────────────────────────────────────────────
    @Test
    void badRequestException_singleArg_shouldReturnMessage() {
        BadRequestException ex = new BadRequestException("bad-request-msg");
        assertNotNull(ex.getMessage());
    }

    @Test
    void badRequestException_withVarArgs_shouldReturnFormattedMessage() {
        BadRequestException ex = new BadRequestException("bad-request-msg", "arg1");
        assertNotNull(ex.getMessage());
    }

    // ── CreateGuestUserException ───────────────────────────────────────────
    @Test
    void createGuestUserException_shouldStoreMessage() {
        CreateGuestUserException ex = new CreateGuestUserException("guest-error");
        assertEquals("guest-error", ex.getMessage());
    }

    // ── DuplicatedException ────────────────────────────────────────────────
    @Test
    void duplicatedException_shouldReturnMessage() {
        DuplicatedException ex = new DuplicatedException("duplicate-error");
        assertNotNull(ex.getMessage());
    }

    @Test
    void duplicatedException_withVarArgs_shouldReturnFormattedMessage() {
        DuplicatedException ex = new DuplicatedException("duplicate-error", "val1");
        assertNotNull(ex.getMessage());
    }

    // ── Forbidden ─────────────────────────────────────────────────────────
    @Test
    void forbidden_shouldReturnMessage() {
        Forbidden ex = new Forbidden("forbidden-error");
        assertNotNull(ex.getMessage());
    }

    @Test
    void forbidden_withVarArgs_shouldReturnFormattedMessage() {
        Forbidden ex = new Forbidden("forbidden-error", "val1");
        assertNotNull(ex.getMessage());
    }

    @Test
    void forbidden_setMessage_shouldUpdateMessage() {
        Forbidden ex = new Forbidden("forbidden-error");
        ex.setMessage("updated");
        assertEquals("updated", ex.getMessage());
    }

    // ── ForbiddenException ─────────────────────────────────────────────────
    @Test
    void forbiddenException_shouldReturnMessage() {
        ForbiddenException ex = new ForbiddenException("forbidden-code");
        assertNotNull(ex.getMessage());
    }

    @Test
    void forbiddenException_withVarArgs_shouldReturnFormattedMessage() {
        ForbiddenException ex = new ForbiddenException("forbidden-code", "val1");
        assertNotNull(ex.getMessage());
    }

    // ── InternalServerErrorException ──────────────────────────────────────
    @Test
    void internalServerErrorException_shouldReturnMessage() {
        InternalServerErrorException ex = new InternalServerErrorException("server-error");
        assertNotNull(ex.getMessage());
    }

    @Test
    void internalServerErrorException_withVarArgs_shouldReturnFormattedMessage() {
        InternalServerErrorException ex = new InternalServerErrorException("server-error", "arg");
        assertNotNull(ex.getMessage());
    }

    // ── MultipartFileContentException ─────────────────────────────────────
    @Test
    void multipartFileContentException_noArg_shouldInstantiate() {
        MultipartFileContentException ex = new MultipartFileContentException();
        assertNotNull(ex);
    }

    @Test
    void multipartFileContentException_withMessage_shouldStoreMessage() {
        MultipartFileContentException ex = new MultipartFileContentException("file-error");
        assertEquals("file-error", ex.getMessage());
    }

    @Test
    void multipartFileContentException_withCause_shouldStoreCause() {
        Throwable cause = new RuntimeException("cause");
        MultipartFileContentException ex = new MultipartFileContentException(cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void multipartFileContentException_withMessageAndCause_shouldStoreBoth() {
        Throwable cause = new RuntimeException("cause");
        MultipartFileContentException ex = new MultipartFileContentException("file-error", cause);
        assertEquals("file-error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // ── NotFoundException ──────────────────────────────────────────────────
    @Test
    void notFoundException_shouldReturnMessage() {
        NotFoundException ex = new NotFoundException("not-found");
        assertNotNull(ex.getMessage());
    }

    @Test
    void notFoundException_withVarArgs_shouldReturnFormattedMessage() {
        NotFoundException ex = new NotFoundException("not-found", "arg1");
        assertNotNull(ex.getMessage());
    }

    // ── ResourceExistedException ───────────────────────────────────────────
    @Test
    void resourceExistedException_shouldReturnMessage() {
        ResourceExistedException ex = new ResourceExistedException("resource-existed");
        assertNotNull(ex.getMessage());
    }

    @Test
    void resourceExistedException_setMessage_shouldUpdateMessage() {
        ResourceExistedException ex = new ResourceExistedException("resource-existed");
        ex.setMessage("new message");
        assertEquals("new message", ex.getMessage());
    }

    // ── SignInRequiredException ────────────────────────────────────────────
    @Test
    void signInRequiredException_shouldReturnMessage() {
        SignInRequiredException ex = new SignInRequiredException("sign-in-required");
        assertNotNull(ex.getMessage());
    }

    @Test
    void signInRequiredException_withVarArgs_shouldReturnFormattedMessage() {
        SignInRequiredException ex = new SignInRequiredException("sign-in-required", "arg");
        assertNotNull(ex.getMessage());
    }

    @Test
    void signInRequiredException_setMessage_shouldUpdateMessage() {
        SignInRequiredException ex = new SignInRequiredException("sign-in-required");
        ex.setMessage("updated");
        assertEquals("updated", ex.getMessage());
    }

    // ── StockExistingException ─────────────────────────────────────────────
    @Test
    void stockExistingException_shouldReturnMessage() {
        StockExistingException ex = new StockExistingException("stock-existing");
        assertNotNull(ex.getMessage());
    }

    @Test
    void stockExistingException_withVarArgs_shouldReturnFormattedMessage() {
        StockExistingException ex = new StockExistingException("stock-existing", "arg");
        assertNotNull(ex.getMessage());
    }

    // ── UnsupportedMediaTypeException ─────────────────────────────────────
    @Test
    void unsupportedMediaTypeException_noArg_shouldInstantiate() {
        UnsupportedMediaTypeException ex = new UnsupportedMediaTypeException();
        assertNotNull(ex);
    }

    @Test
    void unsupportedMediaTypeException_withMessage_shouldStoreMessage() {
        UnsupportedMediaTypeException ex = new UnsupportedMediaTypeException("media-type-error");
        assertEquals("media-type-error", ex.getMessage());
    }

    @Test
    void unsupportedMediaTypeException_withCause_shouldStoreCause() {
        Throwable cause = new RuntimeException("cause");
        UnsupportedMediaTypeException ex = new UnsupportedMediaTypeException(cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void unsupportedMediaTypeException_withMessageAndCause_shouldStoreBoth() {
        Throwable cause = new RuntimeException("cause");
        UnsupportedMediaTypeException ex = new UnsupportedMediaTypeException("media-type-error", cause);
        assertEquals("media-type-error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // ── WrongEmailFormatException ──────────────────────────────────────────
    @Test
    void wrongEmailFormatException_shouldReturnMessage() {
        WrongEmailFormatException ex = new WrongEmailFormatException("wrong-email");
        assertNotNull(ex.getMessage());
    }

    @Test
    void wrongEmailFormatException_withVarArgs_shouldReturnFormattedMessage() {
        WrongEmailFormatException ex = new WrongEmailFormatException("wrong-email", "arg");
        assertNotNull(ex.getMessage());
    }
}
