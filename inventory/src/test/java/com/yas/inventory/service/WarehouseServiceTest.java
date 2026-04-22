package com.yas.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.inventory.model.Warehouse;
import com.yas.inventory.model.enumeration.FilterExistInWhSelection;
import com.yas.inventory.repository.StockRepository;
import com.yas.inventory.repository.WarehouseRepository;
import com.yas.inventory.viewmodel.address.AddressDetailVm;
import com.yas.inventory.viewmodel.address.AddressVm;
import com.yas.inventory.viewmodel.product.ProductInfoVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseDetailVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehouseListGetVm;
import com.yas.inventory.viewmodel.warehouse.WarehousePostVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductService productService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private WarehouseService warehouseService;

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
    void findAllWarehouses_whenWarehousesExist_thenMapsViewModels() {
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));

        List<WarehouseGetVm> result = warehouseService.findAllWarehouses();

        assertThat(result).containsExactly(new WarehouseGetVm(1L, "Warehouse 1"));
    }

    @Test
    void getProductWarehouse_whenWarehouseHasProducts_thenFlagsExistingProducts() {
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of(10L));
        when(productService.filterProducts("phone", "sku-1", List.of(10L), FilterExistInWhSelection.YES))
            .thenReturn(List.of(
                new ProductInfoVm(10L, "Phone", "sku-1", true),
                new ProductInfoVm(11L, "Tablet", "sku-2", false)
            ));

        List<ProductInfoVm> result = warehouseService.getProductWarehouse(1L, "phone", "sku-1",
            FilterExistInWhSelection.YES);

        assertThat(result).containsExactly(
            new ProductInfoVm(10L, "Phone", "sku-1", true),
            new ProductInfoVm(11L, "Tablet", "sku-2", false)
        );
    }

    @Test
    void getProductWarehouse_whenWarehouseHasNoProducts_thenReturnsOriginalProductList() {
        List<ProductInfoVm> productInfoVms = List.of(
            new ProductInfoVm(10L, "Phone", "sku-1", true),
            new ProductInfoVm(11L, "Tablet", "sku-2", false)
        );
        when(stockRepository.getProductIdsInWarehouse(1L)).thenReturn(List.of());
        when(productService.filterProducts("phone", "sku-1", List.of(), FilterExistInWhSelection.YES))
            .thenReturn(productInfoVms);

        List<ProductInfoVm> result = warehouseService.getProductWarehouse(1L, "phone", "sku-1",
            FilterExistInWhSelection.YES);

        assertThat(result).isEqualTo(productInfoVms);
    }

    @Test
    void findById_whenWarehouseExists_thenReturnsDetails() {
        AddressDetailVm addressDetailVm = new AddressDetailVm(
            2L,
            "John Doe",
            "123-456-7890",
            "123 Main St",
            "Apt 4B",
            "Metropolis",
            "12345",
            100L,
            "District",
            200L,
            "State",
            300L,
            "Country"
        );
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(locationService.getAddressById(2L)).thenReturn(addressDetailVm);

        WarehouseDetailVm result = warehouseService.findById(1L);

        assertEquals(new WarehouseDetailVm(1L, "Warehouse 1", "John Doe", "123-456-7890",
            "123 Main St", "Apt 4B", "Metropolis", "12345", 100L, 200L, 300L), result);
    }

    @Test
    void findById_whenWarehouseMissing_thenThrowsNotFoundException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> warehouseService.findById(1L));

        assertEquals("The warehouse 1 is not found", exception.getMessage());
    }

    @Test
    void create_whenWarehouseNameIsUnique_thenCreatesWarehouse() {
        WarehousePostVm warehousePostVm = warehousePostVm();
        AddressVm addressVm = new AddressVm(99L, "John Doe", "123-456-7890", "123 Main St", "Metropolis",
            "12345", 100L, 200L, 300L);

        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(false);
        when(locationService.createAddress(any())).thenReturn(addressVm);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Warehouse result = warehouseService.create(warehousePostVm);

        assertThat(result.getName()).isEqualTo("Main Warehouse");
        assertThat(result.getAddressId()).isEqualTo(99L);
    }

    @Test
    void create_whenWarehouseNameExists_thenThrowsDuplicatedException() {
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> warehouseService.create(warehousePostVm()));

        assertEquals("Request name Main Warehouse is already existed", exception.getMessage());
    }

    @Test
    void update_whenWarehouseExists_thenUpdatesWarehouseAndAddress() {
        WarehousePostVm warehousePostVm = warehousePostVm();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("Main Warehouse", 1L)).thenReturn(false);
        doNothing().when(locationService).updateAddress(anyLong(), any());
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        warehouseService.update(warehousePostVm, 1L);

        assertThat(warehouse.getName()).isEqualTo("Main Warehouse");
        verify(locationService).updateAddress(anyLong(), any());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void update_whenWarehouseMissing_thenThrowsNotFoundException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
            () -> warehouseService.update(warehousePostVm(), 1L));

        assertEquals("The warehouse 1 is not found", exception.getMessage());
    }

    @Test
    void update_whenWarehouseNameDuplicates_thenThrowsDuplicatedException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByNameWithDifferentId("Main Warehouse", 1L)).thenReturn(true);

        DuplicatedException exception = assertThrows(DuplicatedException.class,
            () -> warehouseService.update(warehousePostVm(), 1L));

        assertEquals("Request name Main Warehouse is already existed", exception.getMessage());
    }

    @Test
    void delete_whenWarehouseExists_thenDeletesWarehouseAndAddress() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        doNothing().when(warehouseRepository).deleteById(1L);
        doNothing().when(locationService).deleteAddress(2L);

        warehouseService.delete(1L);

        verify(warehouseRepository).deleteById(1L);
        verify(locationService).deleteAddress(2L);
    }

    @Test
    void delete_whenWarehouseMissing_thenThrowsNotFoundException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> warehouseService.delete(1L));

        assertEquals("The warehouse 1 is not found", exception.getMessage());
    }

    @Test
    void getPageableWarehouses_whenWarehousePageHasData_thenReturnsPagedResult() {
        Page<Warehouse> warehousePage = new PageImpl<>(List.of(warehouse), PageRequest.of(0, 10), 1);
        when(warehouseRepository.findAll(PageRequest.of(0, 10))).thenReturn(warehousePage);

        WarehouseListGetVm result = warehouseService.getPageableWarehouses(0, 10);

        assertThat(result.warehouseContent()).containsExactly(new WarehouseGetVm(1L, "Warehouse 1"));
        assertThat(result.pageNo()).isZero();
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isLast()).isTrue();
    }

    private static WarehousePostVm warehousePostVm() {
        return WarehousePostVm.builder()
            .id("WH-1")
            .name("Main Warehouse")
            .contactName("John Doe")
            .phone("123-456-7890")
            .addressLine1("123 Main St")
            .addressLine2("Apt 4B")
            .city("Metropolis")
            .zipCode("12345")
            .districtId(100L)
            .stateOrProvinceId(200L)
            .countryId(300L)
            .build();
    }
}