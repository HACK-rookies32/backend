package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/**
 * 실명확인(신분증 진위확인) 요청.
 *
 * <p>이름·생년월일·신분증 종류·번호·발급일자를 받아 외부 진위확인 기관(mock)에 대조한다.
 */
@Schema(description = "실명확인(신분증 진위확인) 요청")
public record IdentityVerifyRequest(
        @Schema(description = "(필수) 이름", example = "홍길동", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "이름은 필수입니다.") String name,
        @Schema(description = "(필수) 생년월일 6자리(YYMMDD)", example = "900101", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "생년월일은 필수입니다.") String birthDate,
        @Schema(description = "(필수) 신분증 종류 — RESIDENT / DRIVER", example = "RESIDENT", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "신분증 종류는 필수입니다.") String idType,
        @Schema(description = "(필수) 신분증 번호", example = "900101-1234567", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "신분증 번호는 필수입니다.") String idNumber,
        @Schema(description = "(필수) 발급일자(YYYY-MM-DD)", example = "2015-03-20", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "발급일자는 필수입니다.") String issuedDate) {
}