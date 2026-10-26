package com.unified.scheduler.service;

import com.unified.scheduler.dto.CustomerDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {
    List<CustomerDto> getAllActiveCustomers();
    LookupPageResponseDto lookupCustomers(String query, int page, int limit);
    CustomerDto getCustomerById(Long id);
    CustomerDto getCustomerByCode(String customerCode);
    PageResponseDto<CustomerDto> searchCustomers(String query, Pageable pageable);
}
