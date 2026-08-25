package com.example.kidhackbackend.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 자녀 앱이 보고하는 접속 대상(URL/앱) — 역할표 No.5.
 * 접근성 서비스로 수집한 텍스트가 그대로 들어온다.
 */
public record VisitRequest(
        @NotNull Long childId,
        @NotBlank String target
) {
}
