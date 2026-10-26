package com.unified.scheduler.service;

import com.unified.scheduler.domain.Customer;
import com.unified.scheduler.dto.CustomerDto;
import com.unified.scheduler.dto.LookupOptionDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.CustomerRepository;
import com.unified.scheduler.repository.specification.CustomerSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<CustomerDto> getAllActiveCustomers() {
        return customerRepository.findByIsActiveTrueOrderByLastNameAscFirstNameAsc()
            .stream()
            .map(CustomerDto::fromEntity)
            .toList();
    }

    @Override
    public LookupPageResponseDto lookupCustomers(String query, int page, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending()));

        var paged = customerRepository.findAll(CustomerSpecifications.filter(query), pageable);
        List<LookupOptionDto> options = paged.getContent().stream()
            .map(c -> new LookupOptionDto(c.getId(), c.getCustomerCode(), c.getFullName() + " (" + c.getCustomerCode() + ")"))
            .toList();

        return LookupPageResponseDto.of(options, safePage, safeLimit, paged.getTotalElements());
    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        return customerRepository.findById(id)
            .map(CustomerDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    @Override
    public CustomerDto getCustomerByCode(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode)
            .map(CustomerDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with code: " + customerCode));
    }

    @Override
    public PageResponseDto<CustomerDto> searchCustomers(String query, Pageable pageable) {
        var page = customerRepository.findAll(CustomerSpecifications.filter(query), pageable).map(CustomerDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }
}
