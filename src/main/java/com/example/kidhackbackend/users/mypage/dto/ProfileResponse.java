package com.example.kidhackbackend.users.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "내 프로필 정보")
@Getter
@AllArgsConstructor
public class ProfileResponse {

    @Schema(description = "회원 ID", example = "1")
    private Long id;

    @Schema(description = "로그인 아이디", example = "parent01")
    private String loginId;

    @Schema(description = "실명", example = "홍길동")
    private String username;

    @Schema(description = "닉네임", example = "보라")
    private String nickname;

    @Schema(description = "이메일(CHILD 는 null 가능)", example = "parent01@test.com")
    private String email;

    @Schema(description = "전화번호", example = "010-1111-2222")
    private String phoneNumber;

    @Schema(description = "주소", example = "서울시 강남구")
    private String address;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "연결된 부모 회원 ID(PARENT 로그인 시 null)", example = "1")
    private Long parentId;
}
