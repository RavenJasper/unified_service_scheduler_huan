package com.unified.scheduler.service;

import com.unified.scheduler.domain.ServiceType;
import com.unified.scheduler.dto.LookupOptionDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.ServiceTypeDto;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.ServiceTypeRepository;
import com.unified.scheduler.repository.specification.ServiceTypeSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceTypeServiceImpl implements ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    public ServiceTypeServiceImpl(ServiceTypeRepository serviceTypeRepository) {
        this.serviceTypeRepository = serviceTypeRepository;
    }

    @Override
    public List<ServiceTypeDto> getAllActiveServiceTypes() {
        return serviceTypeRepository.findByIsActiveTrueOrderByCodeAscNameAsc()
            .stream()
            .map(ServiceTypeDto::fromEntity)
            .toList();
    }

    @Override
    public LookupPageResponseDto lookupServiceTypes(String query, String skillCode, int page, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by("code").ascending().and(Sort.by("name").ascending()));

        var paged = serviceTypeRepository.findAll(ServiceTypeSpecifications.filter(query, skillCode), pageable);
        List<LookupOptionDto> options = paged.getContent().stream()
            .map(s -> new LookupOptionDto(s.getId(), s.getCode(), s.getName() + " (" + s.getCode() + " - " + s.getStandardDurationMinutes() + " mins)"))
            .toList();

        return LookupPageResponseDto.of(options, safePage, safeLimit, paged.getTotalElements());
    }

    @Override
    public ServiceTypeDto getServiceTypeById(Long id) {
        return serviceTypeRepository.findById(id)
            .map(ServiceTypeDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Service type not found with ID: " + id));
    }

    @Override
    public PageResponseDto<ServiceTypeDto> searchServiceTypes(String query, String skillCode, Pageable pageable) {
        var page = serviceTypeRepository.findAll(ServiceTypeSpecifications.filter(query, skillCode), pageable)
            .map(ServiceTypeDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }
}
