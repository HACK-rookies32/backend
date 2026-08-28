package com.example.kidhackbackend.admin.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 - 고객 정보 수정 요청 (null 인 필드는 미변경)")
public record AdminCustomerUpdateRequest(
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "이메일") String email,
        @Schema(description = "전화번호") String phoneNumber,
        @Schema(description = "주소") String address
) {
}
