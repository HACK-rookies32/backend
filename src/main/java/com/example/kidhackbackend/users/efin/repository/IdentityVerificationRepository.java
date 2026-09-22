package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.IdentityVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {
    Optional<IdentityVerification> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    /** 회원 탈퇴(물리삭제) 시 본인인증 이력 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from IdentityVerification v where v.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}