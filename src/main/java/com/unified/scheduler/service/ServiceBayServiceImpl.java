package com.unified.scheduler.service;

import com.unified.scheduler.domain.ServiceBay;
import com.unified.scheduler.dto.LookupOptionDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceBayDto;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.ServiceBayRepository;
import com.unified.scheduler.repository.specification.ServiceBaySpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceBayServiceImpl implements ServiceBayService {

    private static final Logger log = LoggerFactory.getLogger(ServiceBayServiceImpl.class);
    private final ServiceBayRepository serviceBayRepository;

    public ServiceBayServiceImpl(ServiceBayRepository serviceBayRepository) {
        this.serviceBayRepository = serviceBayRepository;
    }

    @Override
    public List<ServiceBayDto> getServiceBays(Long dealershipId) {
        log.info("Fetching operational service bays, dealershipId filter={}", dealershipId);
        List<ServiceBay> bays = (dealershipId != null)
            ? serviceBayRepository.findByDealershipIdAndIsOperationalTrueOrderByBayNumberAscNameAsc(dealershipId)
            : serviceBayRepository.findByIsOperationalTrueOrderByDealershipIdAscBayNumberAscNameAsc();
        return bays.stream()
            .map(ServiceBayDto::fromEntity)
            .toList();
    }

    @Override
    public LookupPageResponseDto lookupServiceBays(Long dealershipId, String query, int page, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by("bayNumber").ascending().and(Sort.by("name").ascending()));

        var paged = serviceBayRepository.findAll(ServiceBaySpecifications.filter(dealershipId, query), pageable);
        List<LookupOptionDto> options = paged.getContent().stream()
            .map(b -> new LookupOptionDto(b.getId(), "BAY-" + b.getBayNumber(), b.getName() + " (Bay #" + b.getBayNumber() + ")"))
            .toList();

        return LookupPageResponseDto.of(options, safePage, safeLimit, paged.getTotalElements());
    }

    @Override
    public ServiceBayDto getServiceBayById(Long id) {
        log.info("Fetching service bay by id={}", id);
        return serviceBayRepository.findById(id)
            .map(ServiceBayDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Service bay not found with ID: " + id));
    }

    @Override
    public PageResponseDto<ServiceBayDto> searchServiceBays(Long dealershipId, Pageable pageable) {
        log.info("Searching service bays: dealershipId={}, pageable={}", dealershipId, pageable);
        var page = serviceBayRepository.findAll(ServiceBaySpecifications.filter(dealershipId, null), pageable)
            .map(ServiceBayDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }
}
