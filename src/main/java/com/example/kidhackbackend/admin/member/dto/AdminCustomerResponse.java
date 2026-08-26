package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;

import java.time.LocalDateTime;

public record AdminCustomerResponse(
        Long id, String loginId, String username, String nickname, String email,
        String phoneNumber, String address, Role role, Long parentId, boolean isDeleted,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static AdminCustomerResponse from(Member member) {
        return new AdminCustomerResponse(
                member.getId(), member.getLoginId(), member.getUsername(), member.getNickname(),
                member.getEmail(), member.getPhoneNumber(), member.getAddress(), member.getRole(),
                member.getParent() != null ? member.getParent().getId() : null,
                member.isDeleted(), member.getCreatedAt(), member.getUpdatedAt()
        );
    }
}