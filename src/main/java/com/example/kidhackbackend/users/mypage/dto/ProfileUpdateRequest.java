package com.example.kidhackbackend.users.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "프로필 수정 요청 — 모든 필드 선택, null(생략)인 필드는 미변경")
@Getter
public class ProfileUpdateRequest {

    @Schema(description = "(선택) 닉네임", example = "보라")
    private String nickname;

    @Schema(description = "(선택) 전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "(선택) 이메일", example = "test@test.com")
    private String email;

    @Schema(description = "(선택) 주소", example = "서울시 강남구")
    private String address;

    @Schema(description = "(선택) 프로필 이미지 URL")
    private String profileImageUrl;
}
