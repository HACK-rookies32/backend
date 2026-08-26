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
    List<Member> findByParentId(Long parentId);

    Optional<Member> findByEmail(String email);
    Optional<Member> findByUsernameAndEmail(String username, String email);
    Optional<Member> findByLoginIdAndEmail(String loginId, String email);

    // username(실명) + 부모의 이메일로 자녀 계정 찾기 (아이디 찾기용)
    Optional<Member> findByUsernameAndParentEmail(String username, String parentEmail);

    // loginId + 부모의 이메일로 자녀 계정 찾기 (비밀번호 재설정용)
    Optional<Member> findByLoginIdAndParentEmail(String loginId, String parentEmail);
}