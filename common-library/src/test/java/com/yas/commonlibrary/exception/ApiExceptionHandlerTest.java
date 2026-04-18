package com.yas.commonlibrary.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.viewmodel.error.ErrorVm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    @Test
    void handleNotFoundException_shouldReturn404() {
        NotFoundException ex = new NotFoundException("not-found-code");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleBadRequestException_shouldReturn400() {
        BadRequestException ex = new BadRequestException("bad-request-msg");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleBadRequestException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleOtherException_shouldReturn500() {
        Exception ex = new RuntimeException("unexpected error");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleOtherException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleInternalServerErrorException_shouldReturn500() {
        InternalServerErrorException ex = new InternalServerErrorException("server-error");

        ResponseEntity<ErrorVm> response = handler.handleInternalServerErrorException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleConstraintViolation_shouldReturn400() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getRootBeanClass()).thenAnswer(inv -> String.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("field");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorVm> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        List<String> errors = response.getBody().fieldErrors();
        assertEquals(1, errors.size());
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturn400() {
        org.springframework.dao.DataIntegrityViolationException ex =
            new org.springframework.dao.DataIntegrityViolationException("duplicate key");

        ResponseEntity<ErrorVm> response = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleDuplicated_shouldReturn400() {
        DuplicatedException ex = new DuplicatedException("duplicate-code");

        ResponseEntity<ErrorVm> response = handler.handleDuplicated(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleMissingParams_shouldReturn400() {
        MissingServletRequestParameterException ex =
            new MissingServletRequestParameterException("paramName", "String");

        ResponseEntity<ErrorVm> response = handler.handleMissingParams(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleResourceExistedException_shouldReturn409() {
        ResourceExistedException ex = new ResourceExistedException("resource-existed-code");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleResourceExistedException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void handleAccessDeniedException_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("access denied");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void handleWrongEmailFormatException_shouldReturn400() {
        WrongEmailFormatException ex = new WrongEmailFormatException("wrong-email-code");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleWrongEmailFormatException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleCreateGuestUserException_shouldReturn400() {
        CreateGuestUserException ex = new CreateGuestUserException("guest-error");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleCreateGuestUserException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleStockExistingException_shouldReturn400() {
        StockExistingException ex = new StockExistingException("stock-existing-code");
        WebRequest request = mockWebRequest("/api/test");

        ResponseEntity<ErrorVm> response = handler.handleStockExistingException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleSignInRequired_shouldReturn403() {
        SignInRequiredException ex = new SignInRequiredException("sign-in-required");

        ResponseEntity<ErrorVm> response = handler.handleSignInRequired(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private WebRequest mockWebRequest(String path) {
        org.springframework.mock.web.MockHttpServletRequest httpRequest =
            new org.springframework.mock.web.MockHttpServletRequest("GET", path);
        return new org.springframework.web.context.request.ServletWebRequest(httpRequest);
    }
}
