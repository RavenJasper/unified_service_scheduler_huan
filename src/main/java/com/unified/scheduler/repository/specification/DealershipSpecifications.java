package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.Dealership;
import com.unified.scheduler.util.SpecificationUtils;
import org.springframework.data.jpa.domain.Specification;

public final class DealershipSpecifications {

    private DealershipSpecifications() {
    }

    public static Specification<Dealership> filter(String query, String city) {
        Specification<Dealership> spec = (root, q, cb) -> cb.isTrue(root.get("isActive"));

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
        }

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(cb, query, root.get("code"), root.get("name")));
        }

        return spec;
    }
}
