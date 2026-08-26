package com.example.kidhackbackend.users.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;

/** 차단 규칙 응답 (No.6). */
public record BlockRuleResponse(
        Long id,
        Long childId,
        String pattern
) {
    public static BlockRuleResponse from(BlockRule r) {
        return new BlockRuleResponse(r.getId(), r.getChildId(), r.getPattern());
    }
}
