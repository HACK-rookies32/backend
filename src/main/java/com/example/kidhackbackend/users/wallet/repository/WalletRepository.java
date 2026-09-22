package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.Wallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByOwnerId(Long ownerId);

    /**
     * 회원 탈퇴(물리삭제) 시 소유 지갑 정리. 지갑 원장(WalletTransaction)을 먼저 지운 뒤 호출한다.
     * ({@code InstalledAppRepository.deleteByChildId} 와 같은 벌크 DML 컨벤션)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Wallet w where w.ownerId = :ownerId")
    int deleteByOwnerId(@Param("ownerId") Long ownerId);
}
