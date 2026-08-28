package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "커뮤니티 게시글 상세")
@Getter
@AllArgsConstructor
public class PostDetailResponse {

    @Schema(description = "게시글 ID", example = "15")
    private Long postId;

    @Schema(description = "작성자 회원 ID", example = "1")
    private Long authorId;

    @Schema(description = "작성자 닉네임", example = "보라")
    private String authorNickname;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "첨부파일 URL 목록")
    private List<String> attachments;

    @Schema(description = "관련 링크 목록")
    private List<String> links;

    @Schema(description = "좋아요 수", example = "3")
    private long likeCount;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "댓글 목록 (오래된 순)")
    private List<CommentResponse> comments;
}
