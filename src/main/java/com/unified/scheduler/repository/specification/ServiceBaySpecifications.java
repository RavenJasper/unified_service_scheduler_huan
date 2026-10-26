package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.ServiceBay;
import com.unified.scheduler.util.SpecificationUtils;
import org.springframework.data.jpa.domain.Specification;

public final class ServiceBaySpecifications {

    private ServiceBaySpecifications() {
    }

    public static Specification<ServiceBay> filter(Long dealershipId, String query) {
        Specification<ServiceBay> spec = (root, q, cb) -> cb.isTrue(root.get("isOperational"));

        if (dealershipId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("dealership").get("id"), dealershipId));
        }

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(
                cb, query, root.get("name"), root.get("bayNumber").as(String.class)
            ));
        }

        return spec;
    }
}
