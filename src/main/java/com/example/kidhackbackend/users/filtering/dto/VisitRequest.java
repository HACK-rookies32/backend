package com.example.kidhackbackend.users.filtering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 자녀 앱이 보고하는 접속 대상(URL/앱) — 역할표 No.5.
 * 접근성 서비스로 수집한 텍스트가 그대로 들어온다.
 */
@Schema(description = "접속 대상 감지 요청")
public record VisitRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "접속하려는 대상(URL/앱 패키지 등)", example = "https://badsite.com") @NotBlank String target
) {
}
