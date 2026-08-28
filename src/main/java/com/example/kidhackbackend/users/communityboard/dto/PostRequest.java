package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "커뮤니티 게시글 작성/수정 요청")
@Getter
public class PostRequest {

    @Schema(description = "제목", example = "우리 아이 스마트폰 사용 시간 관련 질문")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "첨부파일 URL 목록")
    private List<String> attachments;

    @Schema(description = "관련 링크 목록")
    private List<String> links;
}
