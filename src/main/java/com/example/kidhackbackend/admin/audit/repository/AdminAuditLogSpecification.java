package com.example.kidhackbackend.admin.audit.repository;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.entity.AdminAuditLog;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * 활동 이력 조회 조건 조립.
 *
 * <p>JPQL 의 {@code (:param is null or ...)} 대신 Criteria 로 짠 이유: 네 조건이 전부 선택값이라
 * 넘어오지 않은 조건은 where 절에서 아예 빠지는 편이 실행계획에 유리하고, null 열거형 파라미터의
 * 타입 추론 문제도 피할 수 있다.
 */
public final class AdminAuditLogSpecification {

    private AdminAuditLogSpecification() {
    }

    public static Specification<AdminAuditLog> search(String adminLoginId, AdminActivityType activityType,
                                                      LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (adminLoginId != null && !adminLoginId.isBlank()) {
                // 부분 일치. 대소문자는 구분하지 않는다.
                predicates.add(cb.like(cb.lower(root.get("adminLoginId")),
                        "%" + adminLoginId.trim().toLowerCase() + "%"));
            }
            if (activityType != null) {
                predicates.add(cb.equal(root.get("activityType"), activityType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
