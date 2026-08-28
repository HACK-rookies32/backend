package com.example.kidhackbackend.users.filtering.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 접속 감지 결과 (No.5). */
@Schema(description = "접속 감지 결과")
public record VisitResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "감지된 대상", example = "https://badsite.com") String target,
        @Schema(description = "차단 대상 여부", example = "true") boolean blocked
) {
}
