package com.unified.scheduler.service;

import com.unified.scheduler.dto.DealershipDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DealershipService {
    List<DealershipDto> getAllActiveDealerships();
    LookupPageResponseDto lookupDealerships(String query, String city, int page, int limit);
    DealershipDto getDealershipById(Long id);
    PageResponseDto<DealershipDto> searchDealerships(String query, String city, Pageable pageable);
}
