package com.example.kidhackbackend.users.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import io.swagger.v3.oas.annotations.media.Schema;

/** 차단 규칙 응답 (No.6). */
@Schema(description = "차단 규칙")
public record BlockRuleResponse(
        @Schema(description = "규칙 ID", example = "1") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "차단 패턴", example = "badsite.com") String pattern
) {
    public static BlockRuleResponse from(BlockRule r) {
        return new BlockRuleResponse(r.getId(), r.getChildId(), r.getPattern());
    }
}
