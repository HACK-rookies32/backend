package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.Card;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    /** 본인 카드 목록(최신순). */
    List<Card> findByOwnerIdOrderByIdDesc(Long ownerId);

    /** 소유권 검증용 — id + ownerId 동시 일치. */
    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);

    /** 회원 탈퇴(물리삭제) 시 등록 카드 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Card c where c.ownerId = :ownerId")
    int deleteByOwnerId(@Param("ownerId") Long ownerId);
}
