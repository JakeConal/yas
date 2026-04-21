package com.yas.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.tax.model.TaxClass;
import com.yas.tax.repository.TaxClassRepository;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class TaxClassServiceTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @InjectMocks
    private TaxClassService taxClassService;

    @Test
    void findAllTaxClasses_whenRepositoryReturnsData_thenMapsToViewModels() {
        when(taxClassRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
            .thenReturn(List.of(taxClass(2L, "Beta"), taxClass(1L, "Alpha")));

        List<TaxClassVm> result = taxClassService.findAllTaxClasses();

        assertThat(result).hasSize(2);
        assertEquals("Beta", result.get(0).name());
        assertEquals("Alpha", result.get(1).name());
    }

    @Test
    void findById_whenTaxClassExists_thenReturnsViewModel() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(taxClass(1L, "Standard")));

        TaxClassVm result = taxClassService.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("Standard", result.name());
    }

    @Test
    void findById_whenTaxClassMissing_thenThrowsNotFoundException() {
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.findById(1L));
    }

    @Test
    void create_whenNameIsUnique_thenSavesTaxClass() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "Standard");
        TaxClass saved = taxClass(10L, "Standard");

        when(taxClassRepository.existsByName("Standard")).thenReturn(false);
        when(taxClassRepository.save(any(TaxClass.class))).thenReturn(saved);

        TaxClass result = taxClassService.create(taxClassPostVm);

        assertEquals("Standard", result.getName());
        verify(taxClassRepository).save(any(TaxClass.class));
    }

    @Test
    void create_whenNameExists_thenThrowsDuplicatedException() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "Standard");
        when(taxClassRepository.existsByName("Standard")).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.create(taxClassPostVm));
    }

    @Test
    void update_whenTaxClassExists_thenUpdatesAndSaves() {
        TaxClass existing = taxClass(1L, "Old");
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "New");

        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(false);
        when(taxClassRepository.save(existing)).thenReturn(existing);

        taxClassService.update(taxClassPostVm, 1L);

        assertEquals("New", existing.getName());
        verify(taxClassRepository).save(existing);
    }

    @Test
    void update_whenTaxClassMissing_thenThrowsNotFoundException() {
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "New");
        when(taxClassRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> taxClassService.update(taxClassPostVm, 1L));
    }

    @Test
    void update_whenNameAlreadyExists_thenThrowsDuplicatedException() {
        TaxClass existing = taxClass(1L, "Old");
        TaxClassPostVm taxClassPostVm = new TaxClassPostVm("1", "New");

        when(taxClassRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taxClassRepository.existsByNameNotUpdatingTaxClass("New", 1L)).thenReturn(true);

        assertThrows(DuplicatedException.class, () -> taxClassService.update(taxClassPostVm, 1L));
    }

    @Test
    void delete_whenTaxClassExists_thenDeletes() {
        when(taxClassRepository.existsById(1L)).thenReturn(true);

        taxClassService.delete(1L);

        verify(taxClassRepository).deleteById(1L);
    }

    @Test
    void delete_whenTaxClassMissing_thenThrowsNotFoundException() {
        when(taxClassRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> taxClassService.delete(1L));
    }

    @Test
    void getPageableTaxClasses_whenPageHasData_thenReturnsPagedViewModel() {
        var pageRequest = PageRequest.of(0, 10);
        when(taxClassRepository.findAll(pageRequest))
            .thenReturn(new PageImpl<>(List.of(taxClass(1L, "Standard")), pageRequest, 1));

        TaxClassListGetVm result = taxClassService.getPageableTaxClasses(0, 10);

        assertThat(result.taxClassContent()).hasSize(1);
        assertEquals("Standard", result.taxClassContent().get(0).name());
        assertEquals(0, result.pageNo());
        assertEquals(10, result.pageSize());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(true, result.isLast());
    }

    private static TaxClass taxClass(Long id, String name) {
        TaxClass taxClass = new TaxClass();
        taxClass.setId(id);
        taxClass.setName(name);
        return taxClass;
    }
}