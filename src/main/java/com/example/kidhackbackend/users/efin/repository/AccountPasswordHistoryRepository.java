package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.AccountPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPasswordHistoryRepository extends JpaRepository<AccountPasswordHistory, Long> {
    List<AccountPasswordHistory> findByMemberId(Long memberId);
}