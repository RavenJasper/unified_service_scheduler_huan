package com.unified.scheduler.service;

import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.dto.DealershipDto;
import com.unified.scheduler.dto.LookupOptionDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.DealershipRepository;
import com.unified.scheduler.repository.specification.DealershipSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DealershipServiceImpl implements DealershipService {

    private final DealershipRepository dealershipRepository;

    public DealershipServiceImpl(DealershipRepository dealershipRepository) {
        this.dealershipRepository = dealershipRepository;
    }

    @Override
    public List<DealershipDto> getAllActiveDealerships() {
        return dealershipRepository.findByIsActiveTrueOrderByCodeAscNameAsc()
            .stream()
            .map(DealershipDto::fromEntity)
            .toList();
    }

    @Override
    public LookupPageResponseDto lookupDealerships(String query, String city, int page, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by("code").ascending().and(Sort.by("name").ascending()));

        var paged = dealershipRepository.findAll(DealershipSpecifications.filter(query, city), pageable);
        List<LookupOptionDto> options = paged.getContent().stream()
            .map(d -> new LookupOptionDto(d.getId(), d.getCode(), d.getName() + " (" + d.getCode() + ")"))
            .toList();

        return LookupPageResponseDto.of(options, safePage, safeLimit, paged.getTotalElements());
    }

    @Override
    public DealershipDto getDealershipById(Long id) {
        return dealershipRepository.findById(id)
            .map(DealershipDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Dealership not found with ID: " + id));
    }

    @Override
    public PageResponseDto<DealershipDto> searchDealerships(String query, String city, Pageable pageable) {
        var page = dealershipRepository.findAll(DealershipSpecifications.filter(query, city), pageable)
            .map(DealershipDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }
}
