package com.projeto.th_piscinas_api.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reusable helpers for the "/filter" endpoints and sort/limit in the style
 * of the front's generic repository (field, "-field" for descending order).
 */
public final class QueryUtils {

    private QueryUtils() {
    }

    public static Sort parseSort(String sort, Set<String> allowedFields) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        boolean desc = sort.startsWith("-");
        String field = desc ? sort.substring(1) : sort;
        if (!allowedFields.contains(field)) {
            return Sort.unsorted();
        }
        return desc ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    public static <T> Specification<T> equalsFilter(Map<String, Object> query, Set<String> allowedFields) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null) {
                for (Map.Entry<String, Object> entry : query.entrySet()) {
                    if (allowedFields.contains(entry.getKey()) && entry.getValue() != null) {
                        predicates.add(cb.equal(root.get(entry.getKey()), entry.getValue()));
                    }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
