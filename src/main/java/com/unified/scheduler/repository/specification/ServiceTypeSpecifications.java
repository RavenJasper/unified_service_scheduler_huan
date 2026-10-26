package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.ServiceType;
import com.unified.scheduler.util.SpecificationUtils;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceTypeSpecifications {

    private ServiceTypeSpecifications() {
    }

    public static Specification<ServiceType> filter(String query, String skillCode) {
        Specification<ServiceType> spec = (root, q, cb) -> cb.isTrue(root.get("isActive"));

        if (skillCode != null && !skillCode.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("requiredSkillCode"), skillCode.trim()));
        }

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(cb, query, root.get("code"), root.get("name")));
        }

        return spec;
    }
}
