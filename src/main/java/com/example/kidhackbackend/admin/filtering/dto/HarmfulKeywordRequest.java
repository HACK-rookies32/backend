package com.example.kidhackbackend.admin.filtering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "전역 유해 키워드 추가 요청")
@Getter
public class HarmfulKeywordRequest {

    @Schema(description = "차단할 키워드 또는 URL 조각(최대 255자)", example = "gambling")
    @NotBlank
    @Size(max = 255, message = "키워드는 255자를 넘을 수 없습니다.")
    private String keyword;
}
