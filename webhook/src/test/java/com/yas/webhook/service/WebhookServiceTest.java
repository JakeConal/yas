package com.yas.webhook.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.webhook.integration.api.WebhookApi;
import com.yas.webhook.model.Event;
import com.yas.webhook.model.Webhook;
import com.yas.webhook.model.WebhookEvent;
import com.yas.webhook.model.WebhookEventNotification;
import com.yas.webhook.model.dto.WebhookEventNotificationDto;
import com.yas.webhook.model.enums.NotificationStatus;
import com.yas.webhook.model.mapper.WebhookMapper;
import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookDetailVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookListGetVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookPostVm;
import com.yas.webhook.model.viewmodel.webhook.WebhookVm;
import com.yas.webhook.repository.EventRepository;
import com.yas.webhook.repository.WebhookEventNotificationRepository;
import com.yas.webhook.repository.WebhookEventRepository;
import com.yas.webhook.repository.WebhookRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookEventNotificationRepository webhookEventNotificationRepository;

    @Mock
    private WebhookMapper webhookMapper;

    @Mock
    private WebhookApi webHookApi;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    void getPageableWebhooks_whenDataExists_thenDelegatesToMapper() {
        Webhook webhook = webhook(1L, "https://example.com/webhook", "json", true);
        Page<Webhook> page = new PageImpl<>(List.of(webhook), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")), 1);
        WebhookListGetVm expected = WebhookListGetVm.builder()
            .webhooks(List.of(webhookVm(webhook)))
            .pageNo(0)
            .pageSize(10)
            .totalElements(1)
            .totalPages(1)
            .isLast(true)
            .build();

        when(webhookRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(webhookMapper.toWebhookListGetVm(page, 0, 10)).thenReturn(expected);

        WebhookListGetVm result = webhookService.getPageableWebhooks(0, 10);

        assertEquals(expected, result);
    }

    @Test
    void findAllWebhooks_whenDataExists_thenMapsToViewModels() {
        Webhook webhook = webhook(1L, "https://example.com/webhook", "json", true);
        WebhookVm webhookVm = webhookVm(webhook);
        when(webhookRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).thenReturn(List.of(webhook));
        when(webhookMapper.toWebhookVm(webhook)).thenReturn(webhookVm);

        List<WebhookVm> result = webhookService.findAllWebhooks();

        assertEquals(List.of(webhookVm), result);
    }

    @Test
    void findById_whenWebhookExists_thenReturnsDetailVm() {
        Webhook webhook = webhook(1L, "https://example.com/webhook", "json", true);
        WebhookDetailVm expected = webhookDetailVm(webhook, List.of());
        when(webhookRepository.findById(1L)).thenReturn(Optional.of(webhook));
        when(webhookMapper.toWebhookDetailVm(webhook)).thenReturn(expected);

        WebhookDetailVm result = webhookService.findById(1L);

        assertEquals(expected, result);
    }

    @Test
    void findById_whenWebhookMissing_thenThrowsNotFoundException() {
        when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.findById(1L));
    }

    @Test
    void create_whenEventsEmpty_thenSavesWebhookWithoutWebhookEvents() {
        WebhookPostVm request = new WebhookPostVm("https://example.com/webhook", "secret", "json", true, List.of());
        Webhook createdWebhook = webhook(1L, request.getPayloadUrl(), request.getContentType(), request.getIsActive());
        WebhookDetailVm expected = webhookDetailVm(createdWebhook, List.of());
        when(webhookMapper.toCreatedWebhook(request)).thenReturn(createdWebhook);
        when(webhookRepository.save(createdWebhook)).thenReturn(createdWebhook);
        when(webhookMapper.toWebhookDetailVm(createdWebhook)).thenReturn(expected);

        WebhookDetailVm result = webhookService.create(request);

        assertEquals(expected, result);
        verifyNoInteractions(eventRepository, webhookEventRepository);
    }

    @Test
    void create_whenEventsExist_thenInitializesAndSavesWebhookEvents() {
        WebhookPostVm request = new WebhookPostVm("https://example.com/webhook", "secret", "json", true,
            List.of(EventVm.builder().id(11L).build()));
        Webhook createdWebhook = webhook(1L, request.getPayloadUrl(), request.getContentType(), request.getIsActive());
        WebhookDetailVm expected = webhookDetailVm(createdWebhook, List.of(EventVm.builder().id(11L).build()));
        Event event = new Event();
        event.setId(11L);

        when(webhookMapper.toCreatedWebhook(request)).thenReturn(createdWebhook);
        when(webhookRepository.save(createdWebhook)).thenReturn(createdWebhook);
        when(eventRepository.findById(11L)).thenReturn(Optional.of(event));
        when(webhookEventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(webhookMapper.toWebhookDetailVm(createdWebhook)).thenReturn(expected);

        WebhookDetailVm result = webhookService.create(request);

        assertEquals(expected, result);
        ArgumentCaptor<List<WebhookEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(webhookEventRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(1L, captor.getValue().get(0).getWebhookId());
        assertEquals(11L, captor.getValue().get(0).getEventId());
    }

    @Test
    void create_whenEventMissing_thenThrowsNotFoundException() {
        WebhookPostVm request = new WebhookPostVm("https://example.com/webhook", "secret", "json", true,
            List.of(EventVm.builder().id(99L).build()));
        Webhook createdWebhook = webhook(1L, request.getPayloadUrl(), request.getContentType(), request.getIsActive());
        when(webhookMapper.toCreatedWebhook(request)).thenReturn(createdWebhook);
        when(webhookRepository.save(createdWebhook)).thenReturn(createdWebhook);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.create(request));
        verify(webhookEventRepository, never()).saveAll(anyList());
    }

    @Test
    void update_whenEventsEmpty_thenDeletesOldEventsAndSkipsSaveAll() {
        List<WebhookEvent> existingEvents = new ArrayList<>();
        existingEvents.add(webhookEvent(1L, 1L, 10L));
        Webhook existingWebhook = webhook(1L, "https://old.example.com", "json", true);
        existingWebhook.setWebhookEvents(existingEvents);

        WebhookPostVm request = new WebhookPostVm("https://new.example.com", "secret2", "json", false, List.of());
        Webhook updatedWebhook = webhook(1L, request.getPayloadUrl(), request.getContentType(), request.getIsActive());

        when(webhookRepository.findById(1L)).thenReturn(Optional.of(existingWebhook));
        when(webhookMapper.toUpdatedWebhook(existingWebhook, request)).thenReturn(updatedWebhook);
        when(webhookRepository.save(updatedWebhook)).thenReturn(updatedWebhook);

        webhookService.update(request, 1L);

        verify(webhookEventRepository).deleteAll(existingEvents);
        verify(webhookEventRepository, never()).saveAll(anyList());
    }

    @Test
    void update_whenEventsExist_thenDeletesAndSavesNewEvents() {
        List<WebhookEvent> existingEvents = new ArrayList<>();
        existingEvents.add(webhookEvent(1L, 1L, 10L));
        Webhook existingWebhook = webhook(1L, "https://old.example.com", "json", true);
        existingWebhook.setWebhookEvents(existingEvents);

        WebhookPostVm request = new WebhookPostVm("https://new.example.com", "secret2", "json", false,
            List.of(EventVm.builder().id(12L).build()));
        Webhook updatedWebhook = webhook(1L, request.getPayloadUrl(), request.getContentType(), request.getIsActive());
        Event event = new Event();
        event.setId(12L);

        when(webhookRepository.findById(1L)).thenReturn(Optional.of(existingWebhook));
        when(webhookMapper.toUpdatedWebhook(existingWebhook, request)).thenReturn(updatedWebhook);
        when(webhookRepository.save(updatedWebhook)).thenReturn(updatedWebhook);
        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(webhookEventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        webhookService.update(request, 1L);

        verify(webhookEventRepository).deleteAll(existingEvents);
        ArgumentCaptor<List<WebhookEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(webhookEventRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(1L, captor.getValue().get(0).getWebhookId());
        assertEquals(12L, captor.getValue().get(0).getEventId());
    }

    @Test
    void update_whenWebhookMissing_thenThrowsNotFoundException() {
        WebhookPostVm request = new WebhookPostVm("https://new.example.com", "secret2", "json", false, List.of());
        when(webhookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookService.update(request, 1L));
    }

    @Test
    void delete_whenWebhookExists_thenDeletesEventsAndWebhook() {
        when(webhookRepository.existsById(1L)).thenReturn(true);

        webhookService.delete(1L);

        verify(webhookEventRepository).deleteByWebhookId(1L);
        verify(webhookRepository).deleteById(1L);
    }

    @Test
    void delete_whenWebhookMissing_thenThrowsNotFoundException() {
        when(webhookRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> webhookService.delete(1L));
    }

    @Test
    void notifyToWebhook_whenNotificationExists_thenMarksItNotified() {
        ObjectNode payload = new ObjectMapper().createObjectNode().put("status", "ok");
        WebhookEventNotificationDto notificationDto = WebhookEventNotificationDto.builder()
            .notificationId(1L)
            .url("https://example.com/webhook")
            .secret("secret")
            .payload(payload)
            .build();
        WebhookEventNotification notification = new WebhookEventNotification();
        notification.setNotificationStatus(NotificationStatus.NOTIFYING);

        when(webhookEventNotificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        webhookService.notifyToWebhook(notificationDto);

        verify(webHookApi).notify(notificationDto.getUrl(), notificationDto.getSecret(), notificationDto.getPayload());
        assertEquals(NotificationStatus.NOTIFIED, notification.getNotificationStatus());
        verify(webhookEventNotificationRepository).save(notification);
    }

    @Test
    void notifyToWebhook_whenNotificationMissing_thenThrowsNoSuchElementException() {
        ObjectNode payload = new ObjectMapper().createObjectNode().put("status", "ok");
        WebhookEventNotificationDto notificationDto = WebhookEventNotificationDto.builder()
            .notificationId(1L)
            .url("https://example.com/webhook")
            .secret("secret")
            .payload(payload)
            .build();

        when(webhookEventNotificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> webhookService.notifyToWebhook(notificationDto));
        verify(webHookApi).notify(anyString(), anyString(), any());
        verify(webhookEventNotificationRepository, never()).save(any());
    }

    private static Webhook webhook(Long id, String payloadUrl, String contentType, Boolean isActive) {
        Webhook webhook = new Webhook();
        webhook.setId(id);
        webhook.setPayloadUrl(payloadUrl);
        webhook.setContentType(contentType);
        webhook.setIsActive(isActive);
        return webhook;
    }

    private static WebhookVm webhookVm(Webhook webhook) {
        WebhookVm webhookVm = new WebhookVm();
        webhookVm.setId(webhook.getId());
        webhookVm.setPayloadUrl(webhook.getPayloadUrl());
        webhookVm.setContentType(webhook.getContentType());
        webhookVm.setIsActive(webhook.getIsActive());
        return webhookVm;
    }

    private static WebhookDetailVm webhookDetailVm(Webhook webhook, List<EventVm> events) {
        WebhookDetailVm webhookDetailVm = new WebhookDetailVm();
        webhookDetailVm.setId(webhook.getId());
        webhookDetailVm.setPayloadUrl(webhook.getPayloadUrl());
        webhookDetailVm.setContentType(webhook.getContentType());
        webhookDetailVm.setIsActive(webhook.getIsActive());
        webhookDetailVm.setEvents(events);
        return webhookDetailVm;
    }

    private static WebhookEvent webhookEvent(Long id, Long webhookId, Long eventId) {
        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setId(id);
        webhookEvent.setWebhookId(webhookId);
        webhookEvent.setEventId(eventId);
        return webhookEvent;
    }
}
