package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 실명확인 완료 여부(카드 등록 위저드의 단계 판단용). */
@Schema(description = "실명확인 상태")
public record IdentityStatusResponse(
        @Schema(description = "실명확인 완료 여부", example = "true") boolean verified) {
}
