package com.yas.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.PaymentService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void initPayment_whenCalled_thenReturnsServiceResponse() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(new BigDecimal("99.99"))
            .checkoutId("checkout-1")
            .build();
        InitPaymentResponseVm responseVm = InitPaymentResponseVm.builder()
            .status("success")
            .paymentId("payment-1")
            .redirectUrl("https://paypal.example")
            .build();
        when(paymentService.initPayment(requestVm)).thenReturn(responseVm);

        InitPaymentResponseVm result = paymentController.initPayment(requestVm);

        assertEquals(responseVm, result);
        verify(paymentService).initPayment(requestVm);
    }

    @Test
    void capturePayment_whenCalled_thenReturnsServiceResponse() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("token-1")
            .build();
        CapturePaymentResponseVm responseVm = CapturePaymentResponseVm.builder()
            .orderId(1L)
            .checkoutId("checkout-1")
            .amount(new BigDecimal("99.99"))
            .paymentFee(new BigDecimal("1.23"))
            .gatewayTransactionId("gateway-1")
            .paymentMethod(PaymentMethod.PAYPAL)
            .paymentStatus(PaymentStatus.COMPLETED)
            .failureMessage(null)
            .build();
        when(paymentService.capturePayment(requestVm)).thenReturn(responseVm);

        CapturePaymentResponseVm result = paymentController.capturePayment(requestVm);

        assertEquals(responseVm, result);
        verify(paymentService).capturePayment(requestVm);
    }

    @Test
    void cancelPayment_whenCalled_thenReturnsOkResponse() {
        ResponseEntity<String> result = paymentController.cancelPayment();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Payment cancelled", result.getBody());
    }
}