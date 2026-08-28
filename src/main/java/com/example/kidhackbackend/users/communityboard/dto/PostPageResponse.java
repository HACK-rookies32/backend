package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Schema(description = "커뮤니티 게시글 페이지")
@Getter
@AllArgsConstructor
public class PostPageResponse {

    @Schema(description = "게시글 목록")
    private List<PostResponse> content;

    @Schema(description = "현재 페이지 번호(0부터)", example = "0")
    private int page;

    @Schema(description = "페이지당 항목 수", example = "20")
    private int size;

    @Schema(description = "전체 항목 수", example = "42")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;
}
