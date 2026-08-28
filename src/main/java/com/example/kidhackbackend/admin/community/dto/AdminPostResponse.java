package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;

@Schema(description = "관리자 - 커뮤니티 게시글 목록 항목")
@Getter
public class AdminPostResponse {

    @Schema(description = "게시글 ID", example = "15")
    private final Long id;

    @Schema(description = "제목")
    private final String title;

    @Schema(description = "본문")
    private final String content;

    @Schema(description = "작성자 회원 ID", example = "1")
    private final Long memberId;

    @Schema(description = "작성자 닉네임", example = "보라")
    private final String authorNickname;

    @Schema(description = "신고 누적 수", example = "2")
    private final int reportCount;

    @Schema(description = "작성 시각")
    private final LocalDateTime createdAt;

    public AdminPostResponse(CommunityBoard post, int reportCount) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.memberId = post.getMember().getId();
        this.authorNickname = post.getMember().getNickname();
        this.reportCount = reportCount;
        this.createdAt = post.getCreatedAt();
    }
}
