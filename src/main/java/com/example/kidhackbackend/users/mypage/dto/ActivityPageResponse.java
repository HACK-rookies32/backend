package com.example.kidhackbackend.users.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "활동 내역 페이지")
@Getter
@AllArgsConstructor
public class ActivityPageResponse {

    @Schema(description = "활동 내역 목록")
    private List<ActivityResponse> content;

    @Schema(description = "현재 페이지 번호(0부터)", example = "0")
    private int page;

    @Schema(description = "페이지당 항목 수", example = "10")
    private int size;

    @Schema(description = "전체 항목 수", example = "2")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "1")
    private int totalPages;
}
