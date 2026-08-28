package com.example.kidhackbackend.admin.filtering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "전역 유해 키워드 추가 요청")
@Getter
public class HarmfulKeywordRequest {

    @Schema(description = "차단할 키워드 또는 URL 조각", example = "gambling")
    @NotBlank
    private String keyword;
}
