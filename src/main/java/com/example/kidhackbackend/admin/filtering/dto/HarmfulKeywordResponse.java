package com.example.kidhackbackend.admin.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "전역 유해 키워드")
@Getter
@RequiredArgsConstructor
public class HarmfulKeywordResponse {

    @Schema(description = "키워드 ID", example = "1")
    private final Long id;

    @Schema(description = "키워드/URL 조각", example = "gambling")
    private final String keyword;

    @Schema(description = "등록 시각")
    private final LocalDateTime createdAt;

    public static HarmfulKeywordResponse from(HarmfulKeyword k) {
        return new HarmfulKeywordResponse(k.getId(), k.getKeyword(), k.getCreatedAt());
    }
}
