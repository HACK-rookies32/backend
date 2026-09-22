package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.PaymentPassword;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentPasswordRepository extends JpaRepository<PaymentPassword, Long> {
    Optional<PaymentPassword> findByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);

    /** 회원 탈퇴(물리삭제) 시 결제 비밀번호 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PaymentPassword p where p.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}
