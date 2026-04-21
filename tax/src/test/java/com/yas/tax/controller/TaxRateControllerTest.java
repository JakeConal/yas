package com.yas.tax.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class TaxRateControllerTest {

    @Mock
    private TaxRateService taxRateService;

    @InjectMocks
    private TaxRateController taxRateController;

    @Test
    void getPageableTaxRates_whenCalled_thenReturnsBody() {
        TaxRateListGetVm expected = new TaxRateListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(expected);

        ResponseEntity<TaxRateListGetVm> response = taxRateController.getPageableTaxRates(0, 10);

        assertEquals(expected, response.getBody());
    }

    @Test
    void getTaxRate_whenCalled_thenReturnsBody() {
        TaxRateVm expected = new TaxRateVm(1L, 7.5, "90210", 2L, 3L, 4L);
        when(taxRateService.findById(1L)).thenReturn(expected);

        ResponseEntity<TaxRateVm> response = taxRateController.getTaxRate(1L);

        assertEquals(expected, response.getBody());
    }

    @Test
    void createTaxRate_whenCalled_thenCreatesLocationAndBody() {
        TaxRate createdTaxRate = taxRate(42L, 7.5, "90210", 2L, 3L, 4L);
        TaxRatePostVm request = new TaxRatePostVm(7.5, "90210", 2L, 3L, 4L);
        when(taxRateService.createTaxRate(request)).thenReturn(createdTaxRate);

        ResponseEntity<TaxRateVm> response = taxRateController.createTaxRate(
            request,
            UriComponentsBuilder.fromPath("/api"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/tax-rates/42", response.getHeaders().getLocation().toString());
        assertEquals(TaxRateVm.fromModel(createdTaxRate), response.getBody());
    }

    @Test
    void updateTaxRate_whenCalled_thenDelegatesToService() {
        TaxRatePostVm request = new TaxRatePostVm(8.0, "11111", 2L, 3L, 4L);

        ResponseEntity<Void> response = taxRateController.updateTaxRate(1L, request);

        assertEquals(204, response.getStatusCode().value());
        verify(taxRateService).updateTaxRate(request, 1L);
    }

    @Test
    void deleteTaxRate_whenCalled_thenDelegatesToService() {
        ResponseEntity<Void> response = taxRateController.deleteTaxRate(1L);

        assertEquals(204, response.getStatusCode().value());
        verify(taxRateService).delete(1L);
    }

    @Test
    void getTaxPercentByAddress_whenCalled_thenReturnsValue() {
        when(taxRateService.getTaxPercent(2L, 4L, 3L, "90210")).thenReturn(7.5);

        ResponseEntity<Double> response = taxRateController.getTaxPercentByAddress(2L, 4L, 3L, "90210");

        assertEquals(7.5, response.getBody());
    }

    @Test
    void getBatchTaxPercentsByAddress_whenCalled_thenReturnsValue() {
        List<TaxRateVm> expected = List.of(new TaxRateVm(1L, 7.5, "90210", 2L, 3L, 4L));
        when(taxRateService.getBulkTaxRate(List.of(1L, 2L), 4L, 3L, "90210")).thenReturn(expected);

        ResponseEntity<List<TaxRateVm>> response = taxRateController.getBatchTaxPercentsByAddress(
            List.of(1L, 2L), 4L, 3L, "90210");

        assertEquals(expected, response.getBody());
    }

    private static TaxRate taxRate(Long id, Double rate, String zipCode, Long taxClassId,
                                   Long stateOrProvinceId, Long countryId) {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(id);
        taxRate.setRate(rate);
        taxRate.setZipCode(zipCode);
        taxRate.setTaxClass(taxClass(taxClassId, "Standard"));
        taxRate.setStateOrProvinceId(stateOrProvinceId);
        taxRate.setCountryId(countryId);
        return taxRate;
    }

    private static TaxClass taxClass(Long id, String name) {
        TaxClass taxClass = new TaxClass();
        taxClass.setId(id);
        taxClass.setName(name);
        return taxClass;
    }
}