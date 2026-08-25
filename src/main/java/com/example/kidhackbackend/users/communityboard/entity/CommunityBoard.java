package com.example.kidhackbackend.users.communityboard.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import com.example.kidhackbackend.users.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_post")
@Getter
@Setter
public class CommunityBoard extends BaseEntity {

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

    @ElementCollection
    @CollectionTable(name = "community_post_attachment", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "url")
    private List<String> attachments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "community_post_link", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "url")
    private List<String> links = new ArrayList<>();
}