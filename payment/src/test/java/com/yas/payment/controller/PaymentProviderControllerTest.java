package com.yas.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentProviderControllerTest {

    @Mock
    private PaymentProviderService paymentProviderService;

    @InjectMocks
    private PaymentProviderController paymentProviderController;

    @Test
    void create_whenCalled_thenReturnsCreatedResponse() {
        CreatePaymentVm requestVm = createPaymentVm();
        PaymentProviderVm responseVm = new PaymentProviderVm("provider-1", "Paypal", "http://config", 1,
            99L, "icon-url");
        when(paymentProviderService.create(requestVm)).thenReturn(responseVm);

        ResponseEntity<PaymentProviderVm> response = paymentProviderController.create(requestVm);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(responseVm, response.getBody());
        verify(paymentProviderService).create(requestVm);
    }

    @Test
    void update_whenCalled_thenReturnsOkResponse() {
        UpdatePaymentVm requestVm = updatePaymentVm();
        PaymentProviderVm responseVm = new PaymentProviderVm("provider-1", "Paypal", "http://config", 1,
            99L, "icon-url");
        when(paymentProviderService.update(requestVm)).thenReturn(responseVm);

        ResponseEntity<PaymentProviderVm> response = paymentProviderController.update(requestVm);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseVm, response.getBody());
        verify(paymentProviderService).update(requestVm);
    }

    @Test
    void getAll_whenCalled_thenReturnsEnabledProviders() {
        var pageable = PageRequest.of(0, 10);
        PaymentProviderVm responseVm = new PaymentProviderVm("provider-1", "Paypal", "http://config", 1,
            99L, "icon-url");
        when(paymentProviderService.getEnabledPaymentProviders(pageable)).thenReturn(List.of(responseVm));

        ResponseEntity<List<PaymentProviderVm>> response = paymentProviderController.getAll(pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(responseVm), response.getBody());
        verify(paymentProviderService).getEnabledPaymentProviders(pageable);
    }

    private static CreatePaymentVm createPaymentVm() {
        CreatePaymentVm requestVm = new CreatePaymentVm();
        requestVm.setId("provider-1");
        requestVm.setEnabled(true);
        requestVm.setName("Paypal");
        requestVm.setConfigureUrl("http://config");
        requestVm.setLandingViewComponentName("payment-provider-config");
        requestVm.setMediaId(99L);
        return requestVm;
    }

    private static UpdatePaymentVm updatePaymentVm() {
        UpdatePaymentVm requestVm = new UpdatePaymentVm();
        requestVm.setId("provider-1");
        requestVm.setEnabled(true);
        requestVm.setName("Paypal");
        requestVm.setConfigureUrl("http://config");
        requestVm.setLandingViewComponentName("payment-provider-config");
        requestVm.setMediaId(99L);
        return requestVm;
    }
}