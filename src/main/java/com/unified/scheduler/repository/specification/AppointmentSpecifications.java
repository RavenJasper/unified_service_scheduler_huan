package com.unified.scheduler.repository.specification;

import com.unified.scheduler.domain.Appointment;
import com.unified.scheduler.domain.AppointmentStatus;
import com.unified.scheduler.util.SpecificationUtils;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    public static Specification<Appointment> filter(String query, Long dealershipId, Long customerId, Long technicianId, AppointmentStatus status) {
        Specification<Appointment> spec = (root, q, cb) -> {
            if (!Long.class.equals(q.getResultType())) {
                root.fetch("dealership", JoinType.LEFT);
                root.fetch("serviceType", JoinType.LEFT);
                root.fetch("serviceBay", JoinType.LEFT);
                root.fetch("technician", JoinType.LEFT);
                root.fetch("customer", JoinType.LEFT);
            }
            return cb.conjunction();
        };

        if (dealershipId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("dealership").get("id"), dealershipId));
        }

        if (customerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId));
        }

        if (technicianId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("technician").get("id"), technicianId));
        }

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        if (query != null && !query.isBlank()) {
            spec = spec.and((root, q, cb) -> SpecificationUtils.anyContainsIgnoreCase(
                cb, query, root.get("referenceNumber"), root.get("vehicleVin")
            ));
        }

        return spec;
    }
}
