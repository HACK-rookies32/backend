package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.AccountPassword;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPasswordRepository extends JpaRepository<AccountPassword, Long> {
    Optional<AccountPassword> findByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);
}