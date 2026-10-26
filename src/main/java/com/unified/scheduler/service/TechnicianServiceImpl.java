package com.unified.scheduler.service;

import com.unified.scheduler.domain.Technician;
import com.unified.scheduler.dto.LookupOptionDto;
import com.unified.scheduler.dto.LookupPageResponseDto;
import com.unified.scheduler.dto.PageResponseDto;
import com.unified.scheduler.dto.TechnicianDto;
import com.unified.scheduler.exception.ResourceNotFoundException;
import com.unified.scheduler.repository.TechnicianRepository;
import com.unified.scheduler.repository.specification.TechnicianSpecifications;
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
public class TechnicianServiceImpl implements TechnicianService {

    private static final Logger log = LoggerFactory.getLogger(TechnicianServiceImpl.class);
    private final TechnicianRepository technicianRepository;

    public TechnicianServiceImpl(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    @Override
    public List<TechnicianDto> getTechnicians(Long dealershipId) {
        log.info("Fetching active technicians, dealershipId filter={}", dealershipId);
        List<Technician> technicians = (dealershipId != null)
            ? technicianRepository.findActiveByDealershipIdWithSkills(dealershipId)
            : technicianRepository.findAllActiveWithSkills();
        return technicians.stream()
            .map(TechnicianDto::fromEntity)
            .toList();
    }

    @Override
    public LookupPageResponseDto lookupTechnicians(Long dealershipId, String query, String skillCode, int page, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by("employeeCode").ascending().and(Sort.by("lastName").ascending()));

        var paged = technicianRepository.findAll(TechnicianSpecifications.filter(dealershipId, query, skillCode), pageable);
        List<LookupOptionDto> options = paged.getContent().stream()
            .map(t -> new LookupOptionDto(t.getId(), t.getEmployeeCode(), t.getFullName() + " (" + t.getEmployeeCode() + ")"))
            .toList();

        return LookupPageResponseDto.of(options, safePage, safeLimit, paged.getTotalElements());
    }

    @Override
    public TechnicianDto getTechnicianById(Long id) {
        log.info("Fetching technician with skills by id={}", id);
        return technicianRepository.findByIdWithSkills(id)
            .map(TechnicianDto::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found with ID: " + id));
    }

    @Override
    public PageResponseDto<TechnicianDto> searchTechnicians(String query, Long dealershipId, String skillCode, Pageable pageable) {
        log.info("Searching technicians: query={}, dealershipId={}, skillCode={}, pageable={}", query, dealershipId, skillCode, pageable);
        var page = technicianRepository.findAll(TechnicianSpecifications.filter(dealershipId, query, skillCode), pageable)
            .map(TechnicianDto::fromEntity);
        return PageResponseDto.fromPage(page);
    }
}
