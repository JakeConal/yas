package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.repository.TaxRateRepository;
import com.yas.tax.viewmodel.location.StateOrProvinceAndCountryGetNameVm;
import com.yas.tax.viewmodel.taxrate.TaxRateGetDetailVm;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private TaxClassRepository taxClassRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private TaxRateService taxRateService;

    @Test
    void createTaxRate_whenTaxClassExists_thenSavesRate() {
        TaxClass taxClass = taxClass(1L, "Standard");
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(7.5, "90210", 1L, 5L, 44L);

        when(taxClassRepository.existsById(1L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(1L)).thenReturn(taxClass);
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaxRate result = taxRateService.createTaxRate(taxRatePostVm);

        assertEquals(7.5, result.getRate());
        assertEquals("90210", result.getZipCode());
        assertEquals(1L, result.getTaxClass().getId());
        assertEquals(5L, result.getStateOrProvinceId());
        assertEquals(44L, result.getCountryId());
    }

    @Test
    void createTaxRate_whenTaxClassMissing_thenThrowsNotFoundException() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(7.5, "90210", 1L, 5L, 44L);
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.createTaxRate(taxRatePostVm));
        verifyNoInteractions(taxRateRepository);
    }

    @Test
    void updateTaxRate_whenTaxRateAndTaxClassExist_thenUpdatesAndSaves() {
        TaxClass oldTaxClass = taxClass(1L, "Old");
        TaxClass newTaxClass = taxClass(2L, "New");
        TaxRate taxRate = TaxRate.builder()
            .id(10L)
            .rate(5.0)
            .zipCode("11111")
            .taxClass(oldTaxClass)
            .stateOrProvinceId(3L)
            .countryId(4L)
            .build();
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(8.0, "22222", 2L, 6L, 7L);

        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(2L)).thenReturn(true);
        when(taxClassRepository.getReferenceById(2L)).thenReturn(newTaxClass);
        when(taxRateRepository.save(taxRate)).thenReturn(taxRate);

        taxRateService.updateTaxRate(taxRatePostVm, 10L);

        assertEquals(8.0, taxRate.getRate());
        assertEquals("22222", taxRate.getZipCode());
        assertEquals(2L, taxRate.getTaxClass().getId());
        assertEquals(6L, taxRate.getStateOrProvinceId());
        assertEquals(7L, taxRate.getCountryId());
        verify(taxRateRepository).save(taxRate);
    }

    @Test
    void updateTaxRate_whenTaxRateMissing_thenThrowsNotFoundException() {
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(8.0, "22222", 2L, 6L, 7L);
        when(taxRateRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(taxRatePostVm, 10L));
    }

    @Test
    void updateTaxRate_whenTaxClassMissing_thenThrowsNotFoundException() {
        TaxRate taxRate = TaxRate.builder()
            .id(10L)
            .rate(5.0)
            .zipCode("11111")
            .taxClass(taxClass(1L, "Old"))
            .stateOrProvinceId(3L)
            .countryId(4L)
            .build();
        TaxRatePostVm taxRatePostVm = new TaxRatePostVm(8.0, "22222", 2L, 6L, 7L);

        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));
        when(taxClassRepository.existsById(2L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.updateTaxRate(taxRatePostVm, 10L));
    }

    @Test
    void delete_whenTaxRateExists_thenDeletes() {
        when(taxRateRepository.existsById(10L)).thenReturn(true);

        taxRateService.delete(10L);

        verify(taxRateRepository).deleteById(10L);
    }

    @Test
    void delete_whenTaxRateMissing_thenThrowsNotFoundException() {
        when(taxRateRepository.existsById(10L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxRateService.delete(10L));
    }

    @Test
    void findById_whenTaxRateExists_thenReturnsViewModel() {
        TaxRate taxRate = TaxRate.builder()
            .id(10L)
            .rate(8.0)
            .zipCode("22222")
            .taxClass(taxClass(2L, "New"))
            .stateOrProvinceId(6L)
            .countryId(7L)
            .build();
        when(taxRateRepository.findById(10L)).thenReturn(Optional.of(taxRate));

        TaxRateVm result = taxRateService.findById(10L);

        assertEquals(10L, result.id());
        assertEquals(2L, result.taxClassId());
        assertEquals(6L, result.stateOrProvinceId());
        assertEquals(7L, result.countryId());
    }

    @Test
    void findAll_whenRatesExist_thenMapsViewModels() {
        TaxRate taxRate = TaxRate.builder()
            .id(10L)
            .rate(8.0)
            .zipCode("22222")
            .taxClass(taxClass(2L, "New"))
            .stateOrProvinceId(6L)
            .countryId(7L)
            .build();
        when(taxRateRepository.findAll()).thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.findAll();

        assertThat(result).hasSize(1);
        assertEquals(2L, result.get(0).taxClassId());
    }

    @Test
    void getPageableTaxRates_whenRatesExist_thenUsesLocationNames() {
        TaxRate firstRate = TaxRate.builder()
            .id(1L)
            .rate(5.0)
            .zipCode("11111")
            .taxClass(taxClass(1L, "Standard"))
            .stateOrProvinceId(101L)
            .countryId(201L)
            .build();
        TaxRate secondRate = TaxRate.builder()
            .id(2L)
            .rate(10.0)
            .zipCode("22222")
            .taxClass(taxClass(2L, "Reduced"))
            .stateOrProvinceId(102L)
            .countryId(202L)
            .build();
        var pageRequest = PageRequest.of(0, 5);
        when(taxRateRepository.findAll(pageRequest))
            .thenReturn(new PageImpl<>(List.of(firstRate, secondRate), pageRequest, 2));
        when(locationService.getStateOrProvinceAndCountryNames(List.of(101L, 102L))).thenReturn(List.of(
            new StateOrProvinceAndCountryGetNameVm(101L, "State 1", "Country 1"),
            new StateOrProvinceAndCountryGetNameVm(102L, "State 2", "Country 2")
        ));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 5);

        assertThat(result.taxRateGetDetailContent()).hasSize(2);
        TaxRateGetDetailVm firstDetail = result.taxRateGetDetailContent().get(0);
        assertEquals("Standard", firstDetail.taxClassName());
        assertEquals("State 1", firstDetail.stateOrProvinceName());
        assertEquals("Country 1", firstDetail.countryName());
    }

    @Test
    void getPageableTaxRates_whenPageIsEmpty_thenSkipsLocationLookup() {
        var pageRequest = PageRequest.of(0, 5);
        when(taxRateRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        TaxRateListGetVm result = taxRateService.getPageableTaxRates(0, 5);

        assertThat(result.taxRateGetDetailContent()).isEmpty();
        verifyNoInteractions(locationService);
    }

    @Test
    void getTaxPercent_whenRepositoryReturnsValue_thenReturnsIt() {
        when(taxRateRepository.getTaxPercent(44L, 5L, "90210", 1L)).thenReturn(7.5);

        double result = taxRateService.getTaxPercent(1L, 44L, 5L, "90210");

        assertEquals(7.5, result);
    }

    @Test
    void getTaxPercent_whenRepositoryReturnsNull_thenReturnsZero() {
        when(taxRateRepository.getTaxPercent(44L, 5L, "90210", 1L)).thenReturn(null);

        double result = taxRateService.getTaxPercent(1L, 44L, 5L, "90210");

        assertEquals(0.0, result);
    }

    @Test
    void getBulkTaxRate_whenRatesExist_thenMapsViewModels() {
        TaxRate taxRate = TaxRate.builder()
            .id(1L)
            .rate(5.0)
            .zipCode("11111")
            .taxClass(taxClass(1L, "Standard"))
            .stateOrProvinceId(101L)
            .countryId(201L)
            .build();

        when(taxRateRepository.getBatchTaxRates(44L, 5L, "90210", new HashSet<>(List.of(1L, 2L))))
            .thenReturn(List.of(taxRate));

        List<TaxRateVm> result = taxRateService.getBulkTaxRate(List.of(1L, 2L), 44L, 5L, "90210");

        assertThat(result).hasSize(1);
        assertEquals(1L, result.get(0).taxClassId());
    }

    private static TaxClass taxClass(Long id, String name) {
        TaxClass taxClass = new TaxClass();
        taxClass.setId(id);
        taxClass.setName(name);
        return taxClass;
    }
}