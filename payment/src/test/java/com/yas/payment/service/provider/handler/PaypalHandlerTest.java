package com.yas.payment.service.provider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaypalHandlerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaypalService paypalService;

    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
    }

    @Test
    void getProviderId_returnsPaypal() {
        assertEquals(PaymentMethod.PAYPAL.name(), paypalHandler.getProviderId());
    }

    @Test
    void initPayment_whenCalled_thenMapsRequestAndResponse() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(new BigDecimal("199.99"))
            .checkoutId("checkout-1")
            .build();
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("payment-settings");
        when(paypalService.createPayment(any())).thenReturn(PaypalCreatePaymentResponse.builder()
            .status("success")
            .paymentId("payment-1")
            .redirectUrl("https://paypal.example/approve")
            .build());

        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        ArgumentCaptor<PaypalCreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaypalCreatePaymentRequest.class);
        verify(paypalService).createPayment(requestCaptor.capture());
        verify(paymentProviderService).getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name());

        PaypalCreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertEquals(new BigDecimal("199.99"), capturedRequest.totalPrice());
        assertEquals("checkout-1", capturedRequest.checkoutId());
        assertEquals(PaymentMethod.PAYPAL.name(), capturedRequest.paymentMethod());
        assertEquals("payment-settings", capturedRequest.paymentSettings());

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getPaymentId()).isEqualTo("payment-1");
        assertThat(result.getRedirectUrl()).isEqualTo("https://paypal.example/approve");
    }

    @Test
    void capturePayment_whenCalled_thenMapsRequestAndResponse() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("token-1")
            .build();
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("payment-settings");
        when(paypalService.capturePayment(any())).thenReturn(PaypalCapturePaymentResponse.builder()
            .checkoutId("checkout-1")
            .amount(new BigDecimal("199.99"))
            .paymentFee(new BigDecimal("1.23"))
            .gatewayTransactionId("gateway-1")
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .failureMessage(null)
            .build());

        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        ArgumentCaptor<PaypalCapturePaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaypalCapturePaymentRequest.class);
        verify(paypalService).capturePayment(requestCaptor.capture());
        verify(paymentProviderService).getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name());

        PaypalCapturePaymentRequest capturedRequest = requestCaptor.getValue();
        assertEquals("token-1", capturedRequest.token());
        assertEquals("payment-settings", capturedRequest.paymentSettings());

        assertThat(result.getCheckoutId()).isEqualTo("checkout-1");
        assertThat(result.getAmount()).isEqualByComparingTo("199.99");
        assertThat(result.getPaymentFee()).isEqualByComparingTo("1.23");
        assertThat(result.getGatewayTransactionId()).isEqualTo("gateway-1");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();
    }
}