package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "관리자 계정 정보")
@Getter
public class AdminMemberResponse {

    @Schema(description = "회원 ID", example = "2")
    private final Long id;

    @Schema(description = "로그인 아이디", example = "admin01")
    private final String loginId;

    @Schema(description = "실명", example = "관리자")
    private final String username;

    @Schema(description = "닉네임")
    private final String nickname;

    @Schema(description = "이메일")
    private final String email;

    @Schema(description = "전화번호")
    private final String phoneNumber;

    @Schema(description = "역할", example = "GENERAL_ADMIN")
    private final Role role;

    @Schema(description = "생성 일시")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
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
