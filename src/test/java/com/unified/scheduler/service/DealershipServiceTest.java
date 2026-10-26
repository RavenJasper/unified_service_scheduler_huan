package com.unified.scheduler.service;

import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.DealershipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealershipServiceTest {

    @Mock
    private DealershipRepository dealershipRepository;

    @InjectMocks
    private DealershipServiceImpl dealershipService;

    @Test
    @DisplayName("shouldReturnAllActiveDealerships_sortedByCodeAndName")
    void shouldReturnAllActiveDealerships_sortedByCodeAndName() {
        Dealership d1 = new Dealership();
        d1.setId(1L);
        d1.setCode("DLR-A");
        d1.setName("Alpha Dealership");

        when(dealershipRepository.findByIsActiveTrueOrderByCodeAscNameAsc()).thenReturn(List.of(d1));

        var result = dealershipService.getAllActiveDealerships();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("DLR-A");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchDealerships_withPagination")
    void shouldSearchDealerships_withPagination() {
        Dealership d1 = new Dealership();
        d1.setId(1L);
        d1.setCode("DLR-METRO");
        d1.setName("Metro Motors");

        var pageRequest = PageRequest.of(0, 10);
        when(dealershipRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(d1), pageRequest, 1));

        var pagedResult = dealershipService.searchDealerships("Metro", null, pageRequest);
        assertThat(pagedResult.content()).hasSize(1);
        assertThat(pagedResult.totalElements()).isEqualTo(1);
        assertThat(pagedResult.pageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenDealershipNotFound")
    void shouldThrowResourceNotFoundException_whenDealershipNotFound() {
        when(dealershipRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dealershipService.getDealershipById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Dealership not found with ID: 999");
    }
}
