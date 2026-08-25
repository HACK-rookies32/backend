package com.example.kidhackbackend.admin.community.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_post_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false, unique = true)
    private Long postId;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    public PostReport(Long postId) {
        this.postId = postId;
        this.reportCount = 1;
    }

    public void increaseReportCount() {
        this.reportCount++;
    }
}