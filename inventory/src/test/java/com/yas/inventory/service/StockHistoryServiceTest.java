package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.inventory.model.Stock;
import com.yas.inventory.model.StockHistory;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.repository.StockHistoryRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryListVm;
import com.yas.inventory.viewmodel.stockhistory.StockHistoryVm;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryServiceTest {

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private StockHistoryService stockHistoryService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
            .id(1L)
            .name("Warehouse 1")
            .addressId(2L)
            .build();
    }

    @Test
    void createStockHistories_whenNoStockQuantityMatches_thenSavesEmptyList() {
        List<Stock> stocks = List.of(
            stock(1L, 10L, 5L),
            stock(2L, 11L, 8L)
        );
        List<StockQuantityVm> stockQuantityVms = List.of(
            new StockQuantityVm(3L, 1L, "other"),
            new StockQuantityVm(4L, 2L, "other")
        );

        stockHistoryService.createStockHistories(stocks, stockQuantityVms);

        ArgumentCaptor<List<StockHistory>> stockHistoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(stockHistoryCaptor.capture());
        assertThat(stockHistoryCaptor.getValue()).isEmpty();
    }

    @Test
    void createStockHistories_whenStockQuantityMatches_thenSavesHistories() {
        List<Stock> stocks = List.of(
            stock(1L, 10L, 5L),
            stock(2L, 11L, 8L)
        );
        List<StockQuantityVm> stockQuantityVms = List.of(
            new StockQuantityVm(1L, 10L, "Initial stock"),
            new StockQuantityVm(2L, 20L, "Restock")
        );

        stockHistoryService.createStockHistories(stocks, stockQuantityVms);

        ArgumentCaptor<List<StockHistory>> stockHistoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryRepository).saveAll(stockHistoryCaptor.capture());

        List<StockHistory> stockHistories = stockHistoryCaptor.getValue();
        assertThat(stockHistories).hasSize(2);
        assertThat(stockHistories.get(0).getProductId()).isEqualTo(10L);
        assertThat(stockHistories.get(0).getAdjustedQuantity()).isEqualTo(10L);
        assertThat(stockHistories.get(0).getNote()).isEqualTo("Initial stock");
        assertThat(stockHistories.get(0).getWarehouse().getId()).isEqualTo(1L);
        assertThat(stockHistories.get(1).getProductId()).isEqualTo(11L);
        assertThat(stockHistories.get(1).getAdjustedQuantity()).isEqualTo(20L);
        assertThat(stockHistories.get(1).getNote()).isEqualTo("Restock");
        assertThat(stockHistories.get(1).getWarehouse().getId()).isEqualTo(1L);
    }

    @Test
    void getStockHistories_whenHistoryExists_thenReturnsMappedHistoryList() {
        StockHistory stockHistory = StockHistory.builder()
            .id(1L)
            .productId(10L)
            .adjustedQuantity(10L)
            .note("Initial stock")
            .warehouse(warehouse)
            .build();
        when(stockHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedOnDesc(10L, 1L))
            .thenReturn(List.of(stockHistory));
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Product 1", "sku-1", true));

        StockHistoryListVm result = stockHistoryService.getStockHistories(10L, 1L);

        assertThat(result.data()).containsExactly(new StockHistoryVm(1L, "Product 1", 10L, null,
            null, "Initial stock"));
        assertEquals(1, result.data().size());
    }

    private Stock stock(Long id, Long productId, Long quantity) {
        return Stock.builder()
            .id(id)
            .productId(productId)
            .quantity(quantity)
            .warehouse(warehouse)
            .build();
    }
}