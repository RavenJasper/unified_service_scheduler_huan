package com.unified.scheduler.service;

import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.TechnicianDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TechnicianService {
    List<TechnicianDto> getTechnicians(Long dealershipId);
    LookupPageResponseDto lookupTechnicians(Long dealershipId, String query, String skillCode, int page, int limit);
    TechnicianDto getTechnicianById(Long id);
    PageResponseDto<TechnicianDto> searchTechnicians(String query, Long dealershipId, String skillCode, Pageable pageable);
}
