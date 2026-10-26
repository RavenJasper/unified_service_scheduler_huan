package com.unified.scheduler.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Arrays;

/**
 * Utility class for constructing common Spring Data JPA Criteria predicates.
 */
public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /**
     * Builds a case-insensitive LIKE %query% predicate for a single string expression.
     */
    public static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> expression, String query) {
        if (query == null || query.isBlank()) {
            return cb.conjunction();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return cb.like(cb.lower(expression), pattern);
    }

    /**
     * Builds an OR predicate across multiple expressions matching a case-insensitive LIKE %query%.
     */
    @SafeVarargs
    public static Predicate anyContainsIgnoreCase(CriteriaBuilder cb, String query, Expression<String>... expressions) {
        if (query == null || query.isBlank() || expressions.length == 0) {
            return cb.conjunction();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        Predicate[] predicates = Arrays.stream(expressions)
            .map(expr -> cb.like(cb.lower(expr), pattern))
            .toArray(Predicate[]::new);
        return cb.or(predicates);
    }
}
