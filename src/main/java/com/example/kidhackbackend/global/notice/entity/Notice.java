package com.example.kidhackbackend.global.notice.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    private String attachmentUrl; // S3 URL 문자열만 저장 — 업로드 자체는 별도 담당자가 구현 예정

    private String linkUrl;

    @Column(nullable = false)
    private Long authorId;

    public Notice(String title, String content, String attachmentUrl, String linkUrl, Long authorId) {
        this.title = title;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
        this.linkUrl = linkUrl;
        this.authorId = authorId;
    }

    public void update(String title, String content, String attachmentUrl, String linkUrl) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (attachmentUrl != null) this.attachmentUrl = attachmentUrl;
        if (linkUrl != null) this.linkUrl = linkUrl;
    }
}