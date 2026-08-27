package com.example.kidhackbackend.admin.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class HarmfulKeywordRequest {

    /** 차단할 키워드 또는 URL 조각. */
    @NotBlank
    private String keyword;
}
