package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AdminPostDetailResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final Long memberId;
    private final String authorNickname;
    private final int reportCount;
    private final List<String> attachments;
    private final List<String> links;
    private final LocalDateTime createdAt;
    private final List<AdminCommentResponse> comments;

    public AdminPostDetailResponse(CommunityBoard post, int reportCount, List<AdminCommentResponse> comments) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.memberId = post.getMember().getId();
        this.authorNickname = post.getMember().getNickname();
        this.reportCount = reportCount;
        this.attachments = post.getAttachments();
        this.links = post.getLinks();
        this.createdAt = post.getCreatedAt();
        this.comments = comments;
    }
}