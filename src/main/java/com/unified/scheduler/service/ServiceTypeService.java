package com.unified.scheduler.service;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceTypeDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServiceTypeService {
    List<ServiceTypeDto> getAllActiveServiceTypes();
    LookupPageResponseDto lookupServiceTypes(String query, String skillCode, int page, int limit);
    ServiceTypeDto getServiceTypeById(Long id);
    PageResponseDto<ServiceTypeDto> searchServiceTypes(String query, String skillCode, Pageable pageable);
}
