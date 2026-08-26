package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AdminPostResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final Long memberId;
    private final String authorNickname;
    private final int reportCount;
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