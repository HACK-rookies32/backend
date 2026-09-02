package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "커뮤니티 게시글 목록 항목")
@Getter
@AllArgsConstructor
public class PostResponse {

    @Schema(description = "게시글 ID", example = "15")
    private Long postId;

    @Schema(description = "작성자 회원 ID (프론트가 '내 글' 판정에 사용)", example = "1")
    private Long authorId;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문 앞부분. 목록에서 한 줄로 보여 준다", example = "초등 4학년 딸아이 쓰는데요, 평일 2시간으로…")
    private String contentPreview;

    @Schema(description = "첫 번째 첨부 사진 경로. 없으면 null", example = "/api/files/community/a1b2.jpg")
    private String thumbnail;

    @Schema(description = "작성자 닉네임", example = "보라")
    private String authorNickname;

    @Schema(description = "좋아요 수", example = "3")
    private long likeCount;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;
}
