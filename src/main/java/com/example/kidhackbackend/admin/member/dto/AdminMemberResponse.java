package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminMemberResponse {

    private final Long id;
    private final String loginId;
    private final String username;
    private final String nickname;
    private final String email;
    private final String phoneNumber;
    private final Role role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AdminMemberResponse(Member member) {
        this.id = member.getId();
        this.loginId = member.getLoginId();
        this.username = member.getUsername();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.phoneNumber = member.getPhoneNumber();
        this.role = member.getRole();
        this.createdAt = member.getCreatedAt();
        this.updatedAt = member.getUpdatedAt();
    }

    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(member);
    }
}