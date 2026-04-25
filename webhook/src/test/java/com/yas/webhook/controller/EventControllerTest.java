package com.yas.webhook.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.yas.webhook.model.viewmodel.webhook.EventVm;
import com.yas.webhook.service.EventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void listWebhooks_whenCalled_thenReturnsBody() {
        List<EventVm> expected = List.of(EventVm.builder().id(1L).build());
        when(eventService.findAllEvents()).thenReturn(expected);

        ResponseEntity<List<EventVm>> response = eventController.listWebhooks();

        assertEquals(expected, response.getBody());
    }
}