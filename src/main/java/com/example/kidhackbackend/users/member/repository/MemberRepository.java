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

    /**
     * 관리자 회원 지갑 목록(프론트 3차 2번) — 회원 + 잔액을 한 번에.
     *
     * <p><b>회원 기준 LEFT JOIN</b>: 지갑이 아직 없는 회원(충전·이체를 한 번도 받지 않은 자녀)도 목록에 나와야
     * 잔액 조정 대상으로 고를 수 있다. 지갑이 없으면 balance 0, updated_at null.
     *
     * <p>{@code Wallet} 이 {@code Member} 를 FK 로 물지 않아(owner_id 만 보유) 파생 쿼리로는 조인이 안 된다.
     * LIKE 바인딩이 Hibernate 7.x + H2 조합에서 깨지는 문제가 있어 위 {@code searchCustomers} 와 같이 네이티브로 쓴다.
     */
    @Query(value = "SELECT m.id AS userId, m.username AS userName, m.role AS role, "
            + "COALESCE(w.balance, 0) AS balance, w.updated_at AS updatedAt "
            + "FROM member m LEFT JOIN wallet w ON w.owner_id = m.id "
            + "WHERE m.role IN ('PARENT', 'CHILD') "
            + "AND (:role IS NULL OR m.role = :role) "
            + "AND (:keyword IS NULL OR "
            + "     m.username LIKE CONCAT('%', :keyword, '%') OR "
            + "     m.login_id LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY m.id DESC",
            countQuery = "SELECT count(*) FROM member m "
                    + "WHERE m.role IN ('PARENT', 'CHILD') "
                    + "AND (:role IS NULL OR m.role = :role) "
                    + "AND (:keyword IS NULL OR "
                    + "     m.username LIKE CONCAT('%', :keyword, '%') OR "
                    + "     m.login_id LIKE CONCAT('%', :keyword, '%'))",
            nativeQuery = true)
    Page<AdminWalletProjection> searchWalletsWithBalance(@Param("role") String role,
                                                         @Param("keyword") String keyword,
                                                         Pageable pageable);
}