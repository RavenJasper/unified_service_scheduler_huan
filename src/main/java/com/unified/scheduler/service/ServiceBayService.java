package com.unified.scheduler.service;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceBayDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServiceBayService {
    List<ServiceBayDto> getServiceBays(Long dealershipId);
    LookupPageResponseDto lookupServiceBays(Long dealershipId, String query, int page, int limit);
    ServiceBayDto getServiceBayById(Long id);
    PageResponseDto<ServiceBayDto> searchServiceBays(Long dealershipId, Pageable pageable);
}
