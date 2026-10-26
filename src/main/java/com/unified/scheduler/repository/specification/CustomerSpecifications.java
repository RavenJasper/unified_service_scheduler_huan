package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.Customer;
import com.unified.scheduler.util.SpecificationUtils;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> filter(String query) {
        Specification<Customer> spec = (root, q, cb) -> cb.isTrue(root.get("isActive"));

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(
                cb, query, root.get("customerCode"), root.get("firstName"), root.get("lastName"), root.get("email"), root.get("phone")
            ));
        }

        return spec;
    }
}
