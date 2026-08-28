package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 - 고객(회원) 상세")
public record AdminCustomerResponse(
        @Schema(description = "회원 ID", example = "7") Long id,
        @Schema(description = "로그인 아이디", example = "child01") String loginId,
        @Schema(description = "실명", example = "김하늘") String username,
        @Schema(description = "닉네임", example = "하늘이") String nickname,
        @Schema(description = "이메일(null 가능)") String email,
        @Schema(description = "전화번호", example = "010-1234-5678") String phoneNumber,
        @Schema(description = "주소(null 가능)") String address,
        @Schema(description = "역할", example = "CHILD") Role role,
        @Schema(description = "연결된 부모 회원 ID(없으면 null)", example = "1") Long parentId,
        @Schema(description = "소프트 삭제 여부", example = "false") boolean isDeleted,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
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
