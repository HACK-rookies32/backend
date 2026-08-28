package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/** 매장 단말 등록/토큰 발급 요청. */
@Schema(description = "매장 단말 등록/토큰 발급 요청")
public record TerminalIssueRequest(
        @Schema(description = "(필수) 가맹점(매장) 이름", example = "GS25 역삼점",
                requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "가맹점 이름은 필수입니다.") String storeName) {
}
