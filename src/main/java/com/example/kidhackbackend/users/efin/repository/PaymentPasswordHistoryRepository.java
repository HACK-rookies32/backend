package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.PaymentPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentPasswordHistoryRepository extends JpaRepository<PaymentPasswordHistory, Long> {
    List<PaymentPasswordHistory> findByMemberId(Long memberId);

    /** 회원 탈퇴(물리삭제) 시 결제 비밀번호 변경 이력 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PaymentPasswordHistory h where h.memberId = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}
