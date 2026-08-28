package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "커뮤니티 게시글 작성/수정 요청. 작성 시 title·content 필수, 수정(PATCH) 시에는 모두 선택(보낸 필드만 반영).")
@Getter
public class PostRequest {

    @Schema(description = "제목", example = "우리 아이 스마트폰 사용 시간 관련 질문",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "본문", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "첨부 공개 경로 목록(POST /api/community/attachments 응답의 url)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<String> attachments;

    @Schema(description = "관련 링크 목록", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<String> links;
}
