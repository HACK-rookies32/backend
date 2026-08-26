package com.example.kidhackbackend.admin.community.repository;

import com.example.kidhackbackend.admin.community.entity.MemberBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberBanRepository extends JpaRepository<MemberBan, Long> {
    Optional<MemberBan> findByMemberId(Long memberId);
}
