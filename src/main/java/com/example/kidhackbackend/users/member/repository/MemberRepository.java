package com.example.kidhackbackend.users.member.repository;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);

    List<Member> findByRoleIn(Collection<Role> roles);
    List<Member> findByIsDeletedTrueAndUpdatedAtBefore(LocalDateTime cutoff);
}