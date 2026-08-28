package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDateTime;

@Schema(description = "관리자 - 커뮤니티 댓글")
@Getter
public class AdminCommentResponse {

    @Schema(description = "댓글 ID", example = "5")
    private final Long id;

    @Schema(description = "게시글 ID", example = "15")
    private final Long postId;

    @Schema(description = "작성자 회원 ID", example = "1")
    private final Long memberId;

    @Schema(description = "작성자 닉네임", example = "보라")
    private final String authorNickname;

    @Schema(description = "댓글 내용")
    private final String content;

    @Schema(description = "신고 누적 수", example = "1")
    private final int reportCount;

    @Schema(description = "작성 시각")
    private final LocalDateTime createdAt;

    public AdminCommentResponse(CommunityBoardComment comment, int reportCount) {
        this.id = comment.getId();
        this.postId = comment.getBoard().getId();
        this.memberId = comment.getMember().getId();
        this.authorNickname = comment.getMember().getNickname();
        this.content = comment.getContent();
        this.reportCount = reportCount;
        this.createdAt = comment.getCreatedAt();
    }
}
