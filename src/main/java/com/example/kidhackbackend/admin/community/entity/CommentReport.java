package com.example.kidhackbackend.admin.community.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_comment_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false, unique = true)
    private Long commentId;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    public CommentReport(Long commentId) {
        this.commentId = commentId;
        this.reportCount = 1;
    }

    public void increaseReportCount() {
        this.reportCount++;
    }
}