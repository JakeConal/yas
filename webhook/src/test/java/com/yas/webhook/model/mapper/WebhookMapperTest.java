package com.yas.webhook.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.webhook.model.Event;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.enums.EventName;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class WebhookMapperTest {

    private final WebhookMapper webhookMapper = Mappers.getMapper(WebhookMapper.class);

    @Test
    void toWebhookEventVms_whenListIsEmpty_thenReturnsEmptyList() {
        List<EventVm> result = webhookMapper.toWebhookEventVms(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void toWebhookEventVms_whenListHasEvents_thenMapsIds() {
        List<EventVm> result = webhookMapper.toWebhookEventVms(List.of(webhookEvent(1L, 1L, 11L)));

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        assertNull(result.get(0).getName());
    }

    @Test
    void toWebhookListGetVm_whenCalled_thenMapsPagingAndWebhooks() {
        Webhook webhook = webhook(1L, "https://example.com/webhook", "json", true);
        PageImpl<Webhook> page = new PageImpl<>(List.of(webhook), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")), 1);

        WebhookListGetVm result = webhookMapper.toWebhookListGetVm(page, 0, 10);

        assertEquals(0, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1L, result.getTotalPages());
        assertTrue(result.isLast());
        assertEquals(1, result.getWebhooks().size());
        assertEquals("https://example.com/webhook", result.getWebhooks().get(0).getPayloadUrl());
    }

    @Test
    void toWebhookDetailVm_whenWebhookHasEvents_thenMapsEventIds() {
        Webhook webhook = webhook(1L, "https://example.com/webhook", "json", true);
        WebhookEvent webhookEvent = webhookEvent(1L, 1L, 11L);
        webhook.setWebhookEvents(List.of(webhookEvent));

        WebhookDetailVm result = webhookMapper.toWebhookDetailVm(webhook);

        assertEquals(1L, result.getId());
        assertEquals("https://example.com/webhook", result.getPayloadUrl());
        assertEquals("json", result.getContentType());
        assertEquals(true, result.getIsActive());
        assertNull(result.getSecret());
        assertEquals(1, result.getEvents().size());
        assertEquals(11L, result.getEvents().get(0).getId());
    }

    private static Webhook webhook(Long id, String payloadUrl, String contentType, Boolean isActive) {
        Webhook webhook = new Webhook();
        webhook.setId(id);
        webhook.setPayloadUrl(payloadUrl);
        webhook.setContentType(contentType);
        webhook.setIsActive(isActive);
        return webhook;
    }

    private static WebhookEvent webhookEvent(Long id, Long webhookId, Long eventId) {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(id);
        webhookEvent.setWebhookId(webhookId);
        webhookEvent.setEventId(eventId);
        return webhookEvent;
    }
}