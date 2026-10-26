package com.unified.scheduler.service;

import com.unified.scheduler.domain.ServiceType;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.ServiceTypeRepository;
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
class ServiceTypeServiceTest {

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @InjectMocks
    private ServiceTypeServiceImpl serviceTypeService;

    @Test
    @DisplayName("shouldReturnAllActiveServiceTypes_sortedByCodeAndName")
    void shouldReturnAllActiveServiceTypes_sortedByCodeAndName() {
        ServiceType st = new ServiceType();
        st.setId(1L);
        st.setCode("BRAKE_SERVICE");
        st.setName("Brake Pad Replacement");

        when(serviceTypeRepository.findByIsActiveTrueOrderByCodeAscNameAsc()).thenReturn(List.of(st));

        var result = serviceTypeService.getAllActiveServiceTypes();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("BRAKE_SERVICE");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchServiceTypes_withPagination")
    void shouldSearchServiceTypes_withPagination() {
        ServiceType st = new ServiceType();
        st.setId(1L);
        st.setCode("BRAKE_SERVICE");
        st.setName("Brake Pad Replacement");

        var pageRequest = PageRequest.of(0, 10);
        when(serviceTypeRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(st), pageRequest, 1));

        var pagedResult = serviceTypeService.searchServiceTypes("Brake", "BRAKES", pageRequest);
        assertThat(pagedResult.content()).hasSize(1);
        assertThat(pagedResult.totalElements()).isEqualTo(1);
        assertThat(pagedResult.pageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenServiceTypeNotFound")
    void shouldThrowResourceNotFoundException_whenServiceTypeNotFound() {
        when(serviceTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceTypeService.getServiceTypeById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Service type not found with ID: 999");
    }
}
