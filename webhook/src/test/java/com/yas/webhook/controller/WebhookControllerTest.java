package com.yas.webhook.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.service.WebhookService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    @Test
    void getPageableWebhooks_whenCalled_thenReturnsBody() {
        WebhookListGetVm expected = WebhookListGetVm.builder().webhooks(List.of()).pageNo(0).pageSize(10)
            .totalElements(0).totalPages(0).isLast(true).build();
        when(webhookService.getPageableWebhooks(0, 10)).thenReturn(expected);

        ResponseEntity<WebhookListGetVm> response = webhookController.getPageableWebhooks(0, 10);

        assertEquals(expected, response.getBody());
    }

    @Test
    void listWebhooks_whenCalled_thenReturnsBody() {
        List<WebhookVm> expected = List.of(webhookVm(1L, "https://example.com/webhook", "json", true));
        when(webhookService.findAllWebhooks()).thenReturn(expected);

        ResponseEntity<List<WebhookVm>> response = webhookController.listWebhooks();

        assertEquals(expected, response.getBody());
    }

    @Test
    void getWebhook_whenCalled_thenReturnsBody() {
        WebhookDetailVm expected = webhookDetailVm(1L, "https://example.com/webhook", "secret", "json", true,
            List.of(EventVm.builder().id(11L).build()));
        when(webhookService.findById(1L)).thenReturn(expected);

        ResponseEntity<WebhookDetailVm> response = webhookController.getWebhook(1L);

        assertEquals(expected, response.getBody());
    }

    @Test
    void createWebhook_whenCalled_thenReturnsCreatedResponse() {
        WebhookPostVm request = new WebhookPostVm("https://example.com/webhook", "secret", "json", true,
            List.of(EventVm.builder().id(11L).build()));
        WebhookDetailVm expected = webhookDetailVm(1L, request.getPayloadUrl(), request.getSecret(), request.getContentType(),
            request.getIsActive(), request.getEvents());
        when(webhookService.create(request)).thenReturn(expected);

        ResponseEntity<WebhookDetailVm> response = webhookController.createWebhook(request, UriComponentsBuilder.fromPath("/api"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void updateWebhook_whenCalled_thenDelegatesToService() {
        WebhookPostVm request = new WebhookPostVm("https://example.com/webhook", "secret", "json", true, List.of());

        ResponseEntity<Void> response = webhookController.updateWebhook(1L, request);

        assertEquals(204, response.getStatusCode().value());
        verify(webhookService).update(request, 1L);
    }

    @Test
    void deleteWebhook_whenCalled_thenDelegatesToService() {
        ResponseEntity<Void> response = webhookController.deleteWebhook(1L);

        assertEquals(204, response.getStatusCode().value());
        verify(webhookService).delete(1L);
    }

    private static WebhookVm webhookVm(Long id, String payloadUrl, String contentType, Boolean isActive) {
        WebhookVm webhookVm = new WebhookVm();
        webhookVm.setId(id);
        webhookVm.setPayloadUrl(payloadUrl);
        webhookVm.setContentType(contentType);
        webhookVm.setIsActive(isActive);
        return webhookVm;
    }

    private static WebhookDetailVm webhookDetailVm(Long id, String payloadUrl, String secret, String contentType,
                                                   Boolean isActive, List<EventVm> events) {
        WebhookDetailVm webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(id);
        webhookDetailVm.setPayloadUrl(payloadUrl);
        webhookDetailVm.setSecret(secret);
        webhookDetailVm.setContentType(contentType);
        webhookDetailVm.setIsActive(isActive);
        webhookDetailVm.setEvents(events);
        return webhookDetailVm;
    }
}