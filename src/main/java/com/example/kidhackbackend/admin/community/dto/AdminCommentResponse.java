package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AdminCommentResponse {
    private final Long id;
    private final Long postId;
    private final Long memberId;
    private final String authorNickname;
    private final String content;
    private final int reportCount;
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