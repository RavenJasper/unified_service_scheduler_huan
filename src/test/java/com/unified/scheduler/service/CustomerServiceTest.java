package com.unified.scheduler.service;

import com.unified.scheduler.domain.Customer;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.CustomerRepository;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("shouldReturnAllActiveCustomers_sortedByLastNameAndFirstName")
    void shouldReturnAllActiveCustomers_sortedByLastNameAndFirstName() {
        Customer c1 = new Customer(1L, "CUST-001", "John", "Doe", "john@example.com", "555-1234");
        when(customerRepository.findByIsActiveTrueOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(c1));

        var result = customerService.getAllActiveCustomers();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().fullName()).isEqualTo("John Doe");
        assertThat(result.getFirst().customerCode()).isEqualTo("CUST-001");
    }

    @Test
    @DisplayName("shouldGetCustomerById_whenFound")
    void shouldGetCustomerById_whenFound() {
        Long id = 1L;
        Customer c1 = new Customer(id, "CUST-001", "John", "Doe", "john@example.com", "555-1234");
        when(customerRepository.findById(id)).thenReturn(Optional.of(c1));

        var result = customerService.getCustomerById(id);
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.customerCode()).isEqualTo("CUST-001");
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("shouldGetCustomerByCode_whenFound")
    void shouldGetCustomerByCode_whenFound() {
        Customer c1 = new Customer(1L, "CUST-001", "John", "Doe", "john@example.com", "555-1234");
        when(customerRepository.findByCustomerCode("CUST-001")).thenReturn(Optional.of(c1));

        var result = customerService.getCustomerByCode("CUST-001");
        assertThat(result.customerCode()).isEqualTo("CUST-001");
        assertThat(result.fullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenCustomerNotFoundById")
    void shouldThrowResourceNotFoundException_whenCustomerNotFoundById() {
        Long id = 999L;
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer not found with ID");
    }

    @Test
    @DisplayName("shouldThrowResourceNotFoundException_whenCustomerNotFoundByCode")
    void shouldThrowResourceNotFoundException_whenCustomerNotFoundByCode() {
        when(customerRepository.findByCustomerCode("CUST-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerByCode("CUST-999"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Customer not found with code");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("shouldSearchCustomers_withPagination")
    void shouldSearchCustomers_withPagination() {
        Customer c1 = new Customer(2L, "CUST-002", "Alice", "Smith", "alice@example.com", "555-5678");
        var pageRequest = PageRequest.of(0, 10);
        when(customerRepository.findAll(any(Specification.class), eq(pageRequest)))
            .thenReturn(new PageImpl<>(List.of(c1), pageRequest, 1));

        var result = customerService.searchCustomers("Alice", pageRequest);
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.pageNumber()).isEqualTo(0);
    }
}
