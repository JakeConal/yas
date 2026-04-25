package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setSubjectUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderExistsByProductAndUserGetVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import com.yas.order.viewmodel.promotion.PromotionUsageVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, productService, cartService,
            orderMapper, promotionService);
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void getLatestOrders_whenCountIsZero_thenReturnsEmptyList() {
        List<OrderBriefVm> result = orderService.getLatestOrders(0);

        assertThat(result).isEmpty();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getLatestOrders_whenRepositoryReturnsEmpty_thenReturnsEmptyList() {
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of());

        List<OrderBriefVm> result = orderService.getLatestOrders(5);

        assertThat(result).isEmpty();
        verify(orderRepository).getLatestOrders(any(Pageable.class));
    }

    @Test
    void getLatestOrders_whenRepositoryReturnsOrders_thenMapsBriefViewModels() {
        Order order = orderWithBillingAddress(1L, "customer@example.com");
        when(orderRepository.getLatestOrders(any(Pageable.class))).thenReturn(List.of(order));

        List<OrderBriefVm> result = orderService.getLatestOrders(5);

        assertThat(result).containsExactly(OrderBriefVm.fromModel(order));
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenNoProductVariations_thenReturnsPresent() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(10L)).thenReturn(List.of());
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.of(orderForStatusCheck()));

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(10L);

        assertThat(result.isPresent()).isTrue();
        verify(orderRepository).findOne(any(Specification.class));
    }

    @Test
    void isOrderCompletedWithUserIdAndProductId_whenProductVariationsExist_thenReturnsFalse() {
        setSubjectUpSecurityContext("user-1");
        when(productService.getProductVariations(10L)).thenReturn(List.of(
            new ProductVariationVm(11L, "Variant 1", "sku-1"),
            new ProductVariationVm(12L, "Variant 2", "sku-2")
        ));
        when(orderRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        OrderExistsByProductAndUserGetVm result = orderService.isOrderCompletedWithUserIdAndProductId(10L);

        assertThat(result.isPresent()).isFalse();
        verify(orderRepository).findOne(any(Specification.class));
    }

    @Test
    void getMyOrders_whenOrdersExist_thenMapsViewModels() {
        setSubjectUpSecurityContext("user-1");
        Order order = orderForStatusCheck();
        when(orderRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(order));

        List<OrderGetVm> result = orderService.getMyOrders("shoe", OrderStatus.COMPLETED);

        assertThat(result).containsExactly(OrderGetVm.fromModel(order, null));
    }

    @Test
    void findOrderVmByCheckoutId_whenOrderExists_thenReturnsOrderVm() {
        Order order = orderWithItems(1L, "checkout-1");
        List<OrderItem> orderItems = List.of(orderItem(100L, 1L, 11L, "Product 1"));
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(1L)).thenReturn(orderItems);

        OrderGetVm result = orderService.findOrderVmByCheckoutId("checkout-1");

        assertEquals(OrderGetVm.fromModel(order, new HashSet<>(orderItems)), result);
    }

    @Test
    void findOrderVmByCheckoutId_whenOrderMissing_thenThrowsNotFoundException() {
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> orderService.findOrderVmByCheckoutId("checkout-1"));

        assertEquals("Order of checkoutId checkout-1 is not found", exception.getMessage());
    }

    @Test
    void createOrder_whenCalled_thenCreatesOrderAndUpdatesRelatedServices() {
        OrderPostVm orderPostVm = orderPostVm();
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setEmail(orderPostVm.email());
        savedOrder.setNote(orderPostVm.note());
        savedOrder.setTax(orderPostVm.tax());
        savedOrder.setDiscount(orderPostVm.discount());
        savedOrder.setNumberItem(orderPostVm.numberItem());
        savedOrder.setTotalPrice(orderPostVm.totalPrice());
        savedOrder.setCouponCode(orderPostVm.couponCode());
        savedOrder.setDeliveryFee(orderPostVm.deliveryFee());
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        savedOrder.setDeliveryStatus(DeliveryStatus.PREPARING);
        savedOrder.setPaymentStatus(orderPostVm.paymentStatus());
        savedOrder.setCheckoutId(orderPostVm.checkoutId());

        Order orderForAccept = new Order();
        orderForAccept.setId(1L);
        orderForAccept.setOrderStatus(OrderStatus.PENDING);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(1L);
            }
            return order;
        });
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderForAccept));

        ArgumentCaptor<Set<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List<PromotionUsageVm>> promotionUsageCaptor = ArgumentCaptor.forClass(List.class);

        OrderVm result = orderService.createOrder(orderPostVm);

        assertThat(result)
            .hasFieldOrPropertyWithValue("id", 1L)
            .hasFieldOrPropertyWithValue("email", orderPostVm.email())
            .hasFieldOrPropertyWithValue("orderStatus", OrderStatus.PENDING)
            .hasFieldOrPropertyWithValue("paymentStatus", orderPostVm.paymentStatus())
            .hasFieldOrPropertyWithValue("checkoutId", orderPostVm.checkoutId());
        assertThat(result.orderItemVms()).hasSize(1);

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderRepository).findById(1L);
        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        verify(productService).subtractProductStockQuantity(result);
        verify(cartService).deleteCartItems(result);
        verify(promotionService).updateUsagePromotion(promotionUsageCaptor.capture());

        Set<OrderItem> savedItems = orderItemsCaptor.getValue();
        assertThat(savedItems).hasSize(1);
        OrderItem savedItem = savedItems.iterator().next();
        assertThat(savedItem.getOrderId()).isEqualTo(1L);
        assertThat(savedItem.getProductId()).isEqualTo(101L);
        assertThat(savedItem.getProductName()).isEqualTo("Product 1");

        List<PromotionUsageVm> promotionUsageVms = promotionUsageCaptor.getValue();
        assertThat(promotionUsageVms).hasSize(1);
        assertThat(promotionUsageVms.get(0).orderId()).isEqualTo(1L);
        assertThat(promotionUsageVms.get(0).productId()).isEqualTo(101L);
        assertThat(promotionUsageVms.get(0).promotionCode()).isEqualTo("PROMO");
    }

    @Test
    void getAllOrder_whenPageIsEmpty_thenReturnsEmptyOrderList() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(Page.empty(PageRequest.of(0, 10)));

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "shoe",
            List.of(),
            Pair.of("Country", "555-1234"),
            "customer@example.com",
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).isNull();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void getAllOrder_whenPageHasOrders_thenMapsBriefViewModels() {
        Order order = orderWithBillingAddress(1L, "customer@example.com");
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(orderPage);

        OrderListVm result = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "shoe",
            List.of(OrderStatus.COMPLETED),
            Pair.of("Country", "555-1234"),
            "customer@example.com",
            Pair.of(0, 10)
        );

        assertThat(result.orderList()).containsExactly(OrderBriefVm.fromModel(order));
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void updateOrderPaymentStatus_whenCompleted_thenMarksOrderPaid() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(PaymentOrderStatusVm.builder()
            .orderId(1L)
            .orderStatus(OrderStatus.PENDING.getName())
            .paymentId(9L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build());

        assertThat(result).hasFieldOrPropertyWithValue("orderId", 1L)
            .hasFieldOrPropertyWithValue("orderStatus", OrderStatus.PAID.getName())
            .hasFieldOrPropertyWithValue("paymentId", 9L)
            .hasFieldOrPropertyWithValue("paymentStatus", PaymentStatus.COMPLETED.name());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(order.getPaymentId()).isEqualTo(9L);
    }

    @Test
    void updateOrderPaymentStatus_whenPending_thenKeepsCurrentOrderStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(PaymentOrderStatusVm.builder()
            .orderId(1L)
            .orderStatus(OrderStatus.PENDING.getName())
            .paymentId(10L)
            .paymentStatus(PaymentStatus.PENDING.name())
            .build());

        assertThat(result).hasFieldOrPropertyWithValue("orderId", 1L)
            .hasFieldOrPropertyWithValue("orderStatus", OrderStatus.PENDING.getName())
            .hasFieldOrPropertyWithValue("paymentId", 10L)
            .hasFieldOrPropertyWithValue("paymentStatus", PaymentStatus.PENDING.name());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void updateOrderPaymentStatus_whenOrderMissing_thenThrowsNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.updateOrderPaymentStatus(PaymentOrderStatusVm.builder()
            .orderId(99L)
            .orderStatus(OrderStatus.PENDING.getName())
            .paymentId(10L)
            .paymentStatus(PaymentStatus.PENDING.name())
            .build()));
    }

    @Test
    void rejectOrder_whenOrderExists_thenMarksRejected() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.ACCEPTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.rejectOrder(1L, "damaged");

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("damaged");
        verify(orderRepository).save(order);
    }

    @Test
    void rejectOrder_whenOrderMissing_thenThrowsNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.rejectOrder(1L, "damaged"));
    }

    @Test
    void acceptOrder_whenOrderMissing_thenThrowsNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.acceptOrder(1L));
    }

    @Test
    void exportCsv_whenOrderListIsNull_thenExportsHeaderOnly() throws IOException {
        TestOrderService exportService = new TestOrderService(orderRepository, orderItemRepository, productService,
            cartService, orderMapper, promotionService);
        exportService.orderListVm = new OrderListVm(null, 0L, 0);
        OrderRequest orderRequest = orderRequest();

        byte[] result = exportService.exportCsv(orderRequest);

        String csv = new String(result, StandardCharsets.UTF_8);
        assertThat(csv).contains("Order status");
        verifyNoInteractions(orderMapper);
    }

    @Test
    void exportCsv_whenOrderListExists_thenMapsRows() throws IOException {
        TestOrderService exportService = new TestOrderService(orderRepository, orderItemRepository, productService,
            cartService, orderMapper, promotionService);
        OrderBriefVm orderBriefVm = OrderBriefVm.fromModel(orderWithBillingAddress(1L, "customer@example.com"));
        OrderItemCsv orderItemCsv = OrderItemCsv.builder()
            .id(1L)
            .orderStatus(OrderStatus.ACCEPTED)
            .paymentStatus(PaymentStatus.COMPLETED)
            .email("customer@example.com")
            .phone("555-1234")
            .totalPrice(new BigDecimal("99.99"))
            .deliveryStatus(DeliveryStatus.DELIVERED)
            .createdOn(ZonedDateTime.now())
            .build();
        exportService.orderListVm = new OrderListVm(List.of(orderBriefVm), 1L, 1);
        when(orderMapper.toCsv(orderBriefVm)).thenReturn(orderItemCsv);

        byte[] result = exportService.exportCsv(orderRequest());

        String csv = new String(result, StandardCharsets.UTF_8);
        assertThat(csv).contains("Email");
        assertThat(csv).contains("customer@example.com");
        verify(orderMapper).toCsv(orderBriefVm);
    }

    private static Order orderForStatusCheck() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.GRAB_EXPRESS);
        order.setTotalPrice(new BigDecimal("100.00"));
        return order;
    }

    private static Order orderWithBillingAddress(Long id, String email) {
        OrderAddress address = OrderAddress.builder()
            .contactName("Customer")
            .phone("555-1234")
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .city("City")
            .zipCode("12345")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        Order order = new Order();
        order.setId(id);
        order.setEmail(email);
        order.setBillingAddressId(address);
        order.setShippingAddressId(address);
        order.setOrderStatus(OrderStatus.ACCEPTED);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED);
        order.setDeliveryMethod(DeliveryMethod.GRAB_EXPRESS);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setTotalPrice(new BigDecimal("99.99"));
        return order;
    }

    private static Order orderWithItems(Long id, String checkoutId) {
        Order order = orderForStatusCheck();
        order.setId(id);
        order.setCheckoutId(checkoutId);
        return order;
    }

    private static OrderItem orderItem(Long id, Long orderId, Long productId, String productName) {
        return OrderItem.builder()
            .id(id)
            .orderId(orderId)
            .productId(productId)
            .productName(productName)
            .quantity(2)
            .productPrice(new BigDecimal("12.34"))
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("0.50"))
            .taxPercent(new BigDecimal("5.00"))
            .build();
    }

    private static OrderPostVm orderPostVm() {
        OrderAddressPostVm addressPostVm = OrderAddressPostVm.builder()
            .contactName("Customer")
            .phone("555-1234")
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .city("City")
            .zipCode("12345")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        OrderItemPostVm itemPostVm = OrderItemPostVm.builder()
            .productId(101L)
            .productName("Product 1")
            .quantity(2)
            .productPrice(new BigDecimal("12.34"))
            .note("Item note")
            .discountAmount(new BigDecimal("1.00"))
            .taxAmount(new BigDecimal("0.50"))
            .taxPercent(new BigDecimal("5.00"))
            .build();

        return OrderPostVm.builder()
            .checkoutId("checkout-1")
            .email("customer@example.com")
            .shippingAddressPostVm(addressPostVm)
            .billingAddressPostVm(addressPostVm)
            .note("Order note")
            .tax(5.0f)
            .discount(1.0f)
            .numberItem(1)
            .totalPrice(new BigDecimal("99.99"))
            .deliveryFee(new BigDecimal("5.00"))
            .couponCode("PROMO")
            .deliveryMethod(DeliveryMethod.GRAB_EXPRESS)
            .paymentMethod(PaymentMethod.COD)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(itemPostVm))
            .build();
    }

    private static OrderRequest orderRequest() {
        return OrderRequest.builder()
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
    }

    private static final class TestOrderService extends OrderService {
        private OrderListVm orderListVm;

        private TestOrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                 ProductService productService, CartService cartService,
                                 OrderMapper orderMapper, PromotionService promotionService) {
            super(orderRepository, orderItemRepository, productService, cartService, orderMapper, promotionService);
        }

        @Override
        public OrderListVm getAllOrder(Pair<ZonedDateTime, ZonedDateTime> timePair, String productName,
                                       List<OrderStatus> orderStatus, Pair<String, String> billingPair,
                                       String email, Pair<Integer, Integer> infoPage) {
            return orderListVm;
        }
    }
}