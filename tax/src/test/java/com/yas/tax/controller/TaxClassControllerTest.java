package com.yas.tax.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class TaxClassControllerTest {

    @Mock
    private TaxClassService taxClassService;

    @InjectMocks
    private TaxClassController taxClassController;

    @Test
    void getPageableTaxClasses_whenCalled_thenReturnsBody() {
        TaxClassListGetVm expected = new TaxClassListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(expected);

        ResponseEntity<TaxClassListGetVm> response = taxClassController.getPageableTaxClasses(0, 10);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
    }

    @Test
    void listTaxClasses_whenCalled_thenReturnsBody() {
        List<TaxClassVm> expected = List.of(new TaxClassVm(1L, "Standard"));
        when(taxClassService.findAllTaxClasses()).thenReturn(expected);

        ResponseEntity<List<TaxClassVm>> response = taxClassController.listTaxClasses();

        assertEquals(expected, response.getBody());
    }

    @Test
    void getTaxClass_whenCalled_thenReturnsBody() {
        TaxClassVm expected = new TaxClassVm(1L, "Standard");
        when(taxClassService.findById(1L)).thenReturn(expected);

        ResponseEntity<TaxClassVm> response = taxClassController.getTaxClass(1L);

        assertEquals(expected, response.getBody());
    }

    @Test
    void createTaxClass_whenCalled_thenCreatesLocationAndBody() {
        TaxClass createdTaxClass = taxClass(42L, "Standard");
        TaxClassPostVm request = new TaxClassPostVm("1", "Standard");
        when(taxClassService.create(request)).thenReturn(createdTaxClass);

        ResponseEntity<TaxClassVm> response = taxClassController.createTaxClass(
            request,
            UriComponentsBuilder.fromPath("/api"));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/tax-classes/42", response.getHeaders().getLocation().toString());
        assertEquals(TaxClassVm.fromModel(createdTaxClass), response.getBody());
    }

    @Test
    void updateTaxClass_whenCalled_thenDelegatesToService() {
        TaxClassPostVm request = new TaxClassPostVm("1", "Updated");

        ResponseEntity<Void> response = taxClassController.updateTaxClass(1L, request);

        assertEquals(204, response.getStatusCode().value());
        verify(taxClassService).update(request, 1L);
    }

    @Test
    void deleteTaxClass_whenCalled_thenDelegatesToService() {
        ResponseEntity<Void> response = taxClassController.deleteTaxClass(1L);

        assertEquals(204, response.getStatusCode().value());
        verify(taxClassService).delete(1L);
    }

    private static TaxClass taxClass(Long id, String name) {
        TaxClass taxClass = new TaxClass();
        taxClass.setId(id);
        taxClass.setName(name);
        return taxClass;
    }
}