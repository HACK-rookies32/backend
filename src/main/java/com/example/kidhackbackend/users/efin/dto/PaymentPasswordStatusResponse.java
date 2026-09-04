package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 결제 비밀번호 설정 여부(카드 등록 위저드의 단계 판단용). */
@Schema(description = "결제 비밀번호 설정 상태")
public record PaymentPasswordStatusResponse(
        @Schema(description = "설정 완료 여부", example = "true") boolean exists) {
}
