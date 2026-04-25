package com.yas.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.service.OrderService;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderListVm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrderControllerDirectTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getOrders_whenCalled_thenDelegatesToService() {
        ZonedDateTime createdFrom = ZonedDateTime.now().minusDays(1);
        ZonedDateTime createdTo = ZonedDateTime.now();
        List<OrderStatus> orderStatus = List.of(OrderStatus.COMPLETED);
        OrderListVm expected = new OrderListVm(List.of(), 0L, 0);
        when(orderService.getAllOrder(Pair.of(createdFrom, createdTo), "shoe", orderStatus,
            Pair.of("Country", "555-1234"), "customer@example.com", Pair.of(0, 10))).thenReturn(expected);

        ResponseEntity<OrderListVm> response = orderController.getOrders(createdFrom, createdTo, "shoe", orderStatus,
            "555-1234", "customer@example.com", "Country", 0, 10);

        assertEquals(expected, response.getBody());
        verify(orderService).getAllOrder(Pair.of(createdFrom, createdTo), "shoe", orderStatus,
            Pair.of("Country", "555-1234"), "customer@example.com", Pair.of(0, 10));
    }

    @Test
    void exportCsv_whenCalled_thenReturnsAttachmentBytes() throws IOException {
        OrderRequest orderRequest = OrderRequest.builder()
            .createdFrom(ZonedDateTime.now().minusDays(1))
            .createdTo(ZonedDateTime.now())
            .productName("shoe")
            .orderStatus(List.of(OrderStatus.COMPLETED))
            .billingPhoneNumber("555-1234")
            .email("customer@example.com")
            .billingCountry("Country")
            .pageNo(0)
            .pageSize(10)
            .build();
        byte[] csvBytes = "csv-data".getBytes(StandardCharsets.UTF_8);
        when(orderService.exportCsv(orderRequest)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = orderController.exportCsv(orderRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .startsWith("attachment; filename=Orders_")
            .endsWith(".csv");
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals(csvBytes.length, response.getBody().length);
        verify(orderService).exportCsv(orderRequest);
    }
}