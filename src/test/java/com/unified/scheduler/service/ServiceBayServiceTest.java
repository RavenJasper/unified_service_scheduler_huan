package com.unified.scheduler.service;

import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.domain.ServiceBay;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.ServiceBayRepository;
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
class ServiceBayServiceTest {

    @Mock
    private ServiceBayRepository serviceBayRepository;

    @InjectMocks
    private ServiceBayServiceImpl serviceBayService;

    @Test
    @DisplayName("shouldReturnServiceBays_whenDealershipIdProvided")
    void shouldReturnServiceBays_whenDealershipIdProvided() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        ServiceBay bay = new ServiceBay(1L, dealership, 1, "Bay 1");
        when(serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(1L)).thenReturn(List.of(bay));

        var result = serviceBayService.getServiceBays(1L);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Bay 1");
    }

    @Test
    @DisplayName("shouldReturnAllOperationalServiceBays_whenDealershipIdIsNull")
    void shouldReturnAllOperationalServiceBays_whenDealershipIdIsNull() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        ServiceBay bay = new ServiceBay(1L, dealership, 1, "Bay 1");
        when(serviceBayRepository.findByIsOperationalTrueOrderByDealershipIdAscBayNumberAscNameAsc()).thenReturn(List.of(bay));

        var result = serviceBayService.getServiceBays(null);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Bay 1");
    }

    @Test
    @DisplayName("shouldReturnServiceBayById_whenFound")
    void shouldReturnServiceBayById_whenFound() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        ServiceBay bay = new ServiceBay(1L, dealership, 1, "Bay 1");
        when(serviceBayRepository.findById(1L)).thenReturn(Optional.of(bay));

        var result = serviceBayService.getServiceBayById(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.bayNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenServiceBayNotFound")
    void shouldThrowResourceNotFoundException_whenServiceBayNotFound() {
        when(serviceBayRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceBayService.getServiceBayById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Service bay not found with ID: 999");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchServiceBays_withPagination")
    void shouldSearchServiceBays_withPagination() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        ServiceBay bay = new ServiceBay(1L, dealership, 1, "Bay 1");

        var pageRequest = PageRequest.of(0, 10);
        when(serviceBayRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(bay), pageRequest, 1));

        var pagedResult = serviceBayService.searchServiceBays(1L, pageRequest);
        assertThat(pagedResult.content()).hasSize(1);
        assertThat(pagedResult.totalElements()).isEqualTo(1);
        assertThat(pagedResult.pageNumber()).isEqualTo(0);
    }
}
