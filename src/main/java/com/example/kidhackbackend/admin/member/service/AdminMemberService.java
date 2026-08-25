package com.example.kidhackbackend.admin.member.service;

import com.example.kidhackbackend.admin.member.dto.AdminMemberResponse;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final List<Role> ADMIN_ROLES = List.of(
            Role.SUPER_ADMIN, Role.GENERAL_ADMIN, Role.COMMUNITY_ADMIN, Role.INQUIRY_ADMIN
    );

    private final MemberRepository memberRepository;

    public List<AdminMemberResponse> getAdminList() {
        return memberRepository.findByRoleIn(ADMIN_ROLES).stream()
                .map(AdminMemberResponse::from)
                .toList();
    }

    public AdminMemberResponse updateAdminRole(Long id, Role newRole) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 계정입니다."));

        if (!ADMIN_ROLES.contains(member.getRole())) {
            throw new IllegalArgumentException("대상 계정이 관리자가 아닙니다.");
        }

        member.setRole(newRole);
        memberRepository.save(member);

        return AdminMemberResponse.from(member);
    }
}