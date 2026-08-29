package com.example.kidhackbackend.users.inquiry.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import com.example.kidhackbackend.users.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquiry")
@Getter
@Setter
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    /** 첨부 공개 경로 목록. 커뮤니티와 동일하게 여러 개를 허용한다(POST /api/inquiries/upload 응답의 url). */
    @ElementCollection
    @CollectionTable(name = "inquiry_attachment", joinColumns = @JoinColumn(name = "inquiry_id"))
    @Column(name = "url")
    private List<String> attachments = new ArrayList<>();

    private String linkUrl;

    @Column(nullable = false)
    private boolean answered = false;
}
