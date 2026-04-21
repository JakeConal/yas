package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class AbstractWebhookEventNotificationServiceTest {

    @Mock
    private WebhookEventNotificationRepository webhookEventNotificationRepository;

    @Test
    void persistNotification_whenCalled_thenSavesNotification() {
        TestNotificationService service = new TestNotificationService(webhookEventNotificationRepository);
        ObjectNode payload = new ObjectMapper().createObjectNode().put("op", "u");
        WebhookEventNotification saved = new WebhookEventNotification();
        saved.setId(9L);
        when(webhookEventNotificationRepository.save(any(WebhookEventNotification.class))).thenReturn(saved);

        Long result = service.persistNotification(12L, payload);

        assertEquals(9L, result);
    }

    @Test
    void createNotificationDto_whenCalled_thenBuildsDto() {
        TestNotificationService service = new TestNotificationService(webhookEventNotificationRepository);
        Webhook webhook = new Webhook();
        webhook.setPayloadUrl("https://example.com/webhook");
        webhook.setSecret("secret");
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setWebhook(webhook);
        ObjectNode payload = new ObjectMapper().createObjectNode().put("op", "u");

        WebhookEventNotificationDto result = service.createNotificationDto(webhookEvent, payload, 99L);

        assertEquals("https://example.com/webhook", result.getUrl());
        assertEquals("secret", result.getSecret());
        assertEquals(99L, result.getNotificationId());
        assertNotNull(result.getPayload());
    }

    private static final class TestNotificationService extends AbstractWebhookEventNotificationService {
        private final WebhookEventNotificationRepository repository;

        private TestNotificationService(WebhookEventNotificationRepository repository) {
            this.repository = repository;
        }

        @Override
        protected WebhookEventNotificationRepository getWebhookEventNotificationRepository() {
            return repository;
        }

        private Long persistNotification(Long webhookEventId, ObjectNode payload) {
            return super.persistNotification(webhookEventId, payload);
        }

        private WebhookEventNotificationDto createNotificationDto(WebhookEvent webhookEvent, ObjectNode payload,
                                                                  Long notificationId) {
            return super.createNotificationDto(webhookEvent, payload, notificationId);
        }
    }
}