package com.unified.scheduler.service;

import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.domain.Technician;
import com.unified.scheduler.domain.TechnicianSkill;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.TechnicianRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianServiceImpl technicianService;

    @Test
    @DisplayName("shouldReturnTechniciansWithSkills_whenDealershipIdProvided")
    void shouldReturnTechniciansWithSkills_whenDealershipIdProvided() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        Technician tech = new Technician(1L, dealership, "TECH-001", "Bob", "Smith", "bob@dealer.com");
        tech.addSkill(new TechnicianSkill(10L, tech, "BRAKES", LocalDate.now()));

        when(technicianRepository.findActiveByDealershipIdWithSkills(1L)).thenReturn(List.of(tech));

        var result = technicianService.getTechnicians(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().employeeCode()).isEqualTo("TECH-001");
        assertThat(result.getFirst().skills()).hasSize(1);
        assertThat(result.getFirst().skills().getFirst().skillCode()).isEqualTo("BRAKES");
    }

    @Test
    @DisplayName("shouldReturnAllActiveTechniciansWithSkills_whenDealershipIdIsNull")
    void shouldReturnAllActiveTechniciansWithSkills_whenDealershipIdIsNull() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        Technician tech = new Technician(1L, dealership, "TECH-001", "Bob", "Smith", "bob@dealer.com");
        when(technicianRepository.findAllActiveWithSkills()).thenReturn(List.of(tech));

        var result = technicianService.getTechnicians(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().employeeCode()).isEqualTo("TECH-001");
    }

    @Test
    @DisplayName("shouldReturnTechnicianById_whenFound")
    void shouldReturnTechnicianById_whenFound() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        dealership.setName("Metro Motors");

        Technician tech = new Technician(1L, dealership, "TECH-001", "Bob", "Smith", "bob@dealer.com");
        when(technicianRepository.findByIdWithSkills(1L)).thenReturn(Optional.of(tech));

        var result = technicianService.getTechnicianById(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("Bob Smith");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenTechnicianNotFound")
    void shouldThrowResourceNotFoundException_whenTechnicianNotFound() {
        when(technicianRepository.findByIdWithSkills(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> technicianService.getTechnicianById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Technician not found with ID: 999");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchTechnicians_withPagination")
    void shouldSearchTechnicians_withPagination() {
        Dealership dealership = new Dealership();
        dealership.setId(1L);
        Technician tech = new Technician(1L, dealership, "TECH-001", "Bob", "Smith", "bob@dealer.com");

        var pageRequest = PageRequest.of(0, 10);
        when(technicianRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(tech), pageRequest, 1));

        var pagedResult = technicianService.searchTechnicians("Bob", 1L, null, pageRequest);
        assertThat(pagedResult.content()).hasSize(1);
        assertThat(pagedResult.totalElements()).isEqualTo(1);
        assertThat(pagedResult.pageNumber()).isEqualTo(0);
    }
}
