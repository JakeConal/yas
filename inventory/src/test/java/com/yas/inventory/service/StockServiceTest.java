package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.StockExistingException;
import com.yas.inventory.constants.ApiConstant;
import com.yas.inventory.model.Stock;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.product.ProductQuantityPostVm;
import com.yas.inventory.viewmodel.stock.StockPostVm;
import com.yas.inventory.viewmodel.stock.StockQuantityUpdateVm;
import com.yas.inventory.viewmodel.stock.StockQuantityVm;
import com.yas.inventory.viewmodel.stock.StockVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockHistoryService stockHistoryService;

    @InjectMocks
    private StockService stockService;

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
    void addProductIntoWarehouse_whenProductAndWarehouseExist_thenSavesStock() {
        StockPostVm stockPostVm = new StockPostVm(10L, 1L);
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Phone", "sku-1", true));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        stockService.addProductIntoWarehouse(List.of(stockPostVm));

        ArgumentCaptor<List<Stock>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).saveAll(stockCaptor.capture());
        assertThat(stockCaptor.getValue()).hasSize(1);
        Stock savedStock = stockCaptor.getValue().get(0);
        assertThat(savedStock.getProductId()).isEqualTo(10L);
        assertThat(savedStock.getWarehouse()).isEqualTo(warehouse);
        assertThat(savedStock.getQuantity()).isZero();
        assertThat(savedStock.getReservedQuantity()).isZero();
    }

    @Test
    void addProductIntoWarehouse_whenStockAlreadyExists_thenThrowsStockExistingException() {
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 10L)).thenReturn(true);

        StockExistingException exception = assertThrows(StockExistingException.class,
            () -> stockService.addProductIntoWarehouse(List.of(new StockPostVm(10L, 1L))));

        assertEquals("The product id 10 already existing warehouse.", exception.getMessage());
    }

    @Test
    void addProductIntoWarehouse_whenProductMissing_thenThrowsNotFoundException() {
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(null);

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(List.of(new StockPostVm(10L, 1L))));

        assertEquals("The product 10 is not found", exception.getMessage());
    }

    @Test
    void addProductIntoWarehouse_whenWarehouseMissing_thenThrowsNotFoundException() {
        when(stockRepository.existsByWarehouseIdAndProductId(1L, 10L)).thenReturn(false);
        when(productService.getProduct(10L)).thenReturn(new ProductInfoVm(10L, "Phone", "sku-1", true));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> stockService.addProductIntoWarehouse(List.of(new StockPostVm(10L, 1L))));

        assertEquals("The warehouse 1 is not found", exception.getMessage());
    }

    @Test
    void getStocksByWarehouseIdAndProductNameAndSku_whenProductsExist_thenMapsStocks() {
        Stock stock1 = stock(1L, 10L, 1L, 15L, 0L);
        Stock stock2 = stock(2L, 11L, 1L, 20L, 0L);
        when(warehouseService.getProductWarehouse(1L, "Phone", "sku-1", FilterExistInWhSelection.YES))
            .thenReturn(List.of(
                new ProductInfoVm(10L, "Phone", "sku-1", true),
                new ProductInfoVm(11L, "Tablet", "sku-2", true)
            ));
        when(stockRepository.findByWarehouseIdAndProductIdIn(anyLong(), anyList())).thenReturn(List.of(stock1, stock2));

        List<StockVm> result = stockService.getStocksByWarehouseIdAndProductNameAndSku(1L, "Phone", "sku-1");

        assertThat(result).containsExactly(
            new StockVm(1L, 10L, "Phone", "sku-1", 15L, 0L, 1L),
            new StockVm(2L, 11L, "Tablet", "sku-2", 20L, 0L, 1L)
        );
    }

    @Test
    void updateProductQuantityInStock_whenRequestIsValid_thenUpdatesStocksAndProductQuantities() {
        Stock stock1 = stock(1L, 10L, 1L, 10L, 0L);
        Stock stock2 = stock(2L, 11L, 1L, 20L, 0L);
        StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(
            new StockQuantityVm(1L, 5L, "adjust stock")
        ));

        when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock1, stock2));
        when(stockRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        stockService.updateProductQuantityInStock(requestBody);

        ArgumentCaptor<List<ProductQuantityPostVm>> productQuantityCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockHistoryService).createStockHistories(anyList(), anyList());
        verify(productService).updateProductQuantity(productQuantityCaptor.capture());

        assertThat(stock1.getQuantity()).isEqualTo(15L);
        assertThat(stock2.getQuantity()).isEqualTo(20L);
        assertThat(productQuantityCaptor.getValue()).containsExactly(
            new ProductQuantityPostVm(10L, 15L),
            new ProductQuantityPostVm(11L, 20L)
        );
    }

    @Test
    void updateProductQuantityInStock_whenQuantityIsInvalid_thenThrowsBadRequestException() {
        Stock stock = stock(1L, 10L, 1L, -15L, 0L);
        StockQuantityUpdateVm requestBody = new StockQuantityUpdateVm(List.of(
            new StockQuantityVm(1L, -10L, "adjust stock")
        ));

        when(stockRepository.findAllById(anyList())).thenReturn(List.of(stock));

        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> stockService.updateProductQuantityInStock(requestBody));

        assertEquals("Invalid adjusted quantity make a negative quantity", exception.getMessage());
    }

    private static Stock stock(Long id, Long productId, Long warehouseId, Long quantity, Long reservedQuantity) {
        Warehouse warehouse = Warehouse.builder()
            .id(warehouseId)
            .name("Warehouse 1")
            .addressId(2L)
            .build();

        return Stock.builder()
            .id(id)
            .productId(productId)
            .quantity(quantity)
            .reservedQuantity(reservedQuantity)
            .warehouse(warehouse)
            .build();
    }
}