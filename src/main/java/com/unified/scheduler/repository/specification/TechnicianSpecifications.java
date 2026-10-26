package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.Technician;
import com.unified.scheduler.util.SpecificationUtils;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class TechnicianSpecifications {

    private TechnicianSpecifications() {
    }

    public static Specification<Technician> filter(Long dealershipId, String query, String skillCode) {
        Specification<Technician> spec = (root, q, cb) -> {
            q.distinct(true);
            return cb.isTrue(root.get("isActive"));
        };

        if (dealershipId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("dealership").get("id"), dealershipId));
        }

        if (skillCode != null && !skillCode.isBlank()) {
            spec = spec.and((root, q, cb) -> {
                var skillsJoin = root.join("skills", JoinType.INNER);
                return cb.equal(skillsJoin.get("skillCode"), skillCode.trim());
            });
        }

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(
                cb, query, root.get("employeeCode"), root.get("firstName"), root.get("lastName"), root.get("email")
            ));
        }

        return spec;
    }
}
