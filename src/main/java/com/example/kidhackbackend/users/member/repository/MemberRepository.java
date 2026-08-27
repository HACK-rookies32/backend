package com.example.kidhackbackend.users.member.repository;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);

    List<Member> findByRoleIn(Collection<Role> roles);
    List<Member> findByIsDeletedTrueAndUpdatedAtBefore(LocalDateTime cutoff);
    List<Member> findByParentId(Long parentId);

    Optional<Member> findByEmail(String email);
    Optional<Member> findByUsernameAndEmail(String username, String email);
    Optional<Member> findByLoginIdAndEmail(String loginId, String email);

    // username(실명) + 부모의 이메일로 자녀 계정 찾기 (아이디 찾기용)
    Optional<Member> findByUsernameAndParentEmail(String username, String parentEmail);

    // loginId + 부모의 이메일로 자녀 계정 찾기 (비밀번호 재설정용)
    Optional<Member> findByLoginIdAndParentEmail(String loginId, String parentEmail);

    // 전체 알림 발송
    List<Member> findByIsDeletedFalse();

    // 관리자 - 고객(회원) 검색용. PARENT/CHILD만 대상으로 하고, role/keyword는 선택 필터.
// JPQL의 LIKE 파라미터 바인딩이 Hibernate 7.x + H2 조합에서 escape('') 문제로 매칭이 깨져서 네이티브 쿼리로 우회한다.
    @Query(value = "SELECT * FROM member m WHERE m.role IN ('PARENT', 'CHILD') " +
            "AND (:role IS NULL OR m.role = :role) " +
            "AND (:keyword IS NULL OR " +
            "     m.login_id LIKE CONCAT('%', :keyword, '%') OR " +
            "     m.username LIKE CONCAT('%', :keyword, '%') OR " +
            "     m.nickname LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT count(*) FROM member m WHERE m.role IN ('PARENT', 'CHILD') " +
                    "AND (:role IS NULL OR m.role = :role) " +
                    "AND (:keyword IS NULL OR " +
                    "     m.login_id LIKE CONCAT('%', :keyword, '%') OR " +
                    "     m.username LIKE CONCAT('%', :keyword, '%') OR " +
                    "     m.nickname LIKE CONCAT('%', :keyword, '%'))",
            nativeQuery = true)
    Page<Member> searchCustomers(@Param("role") String role, @Param("keyword") String keyword, Pageable pageable);
}