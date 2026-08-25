package com.example.kidhackbackend.users.communityboard.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import com.example.kidhackbackend.users.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "community_post_like", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "member_id"}))
@Getter
@Setter
public class CommunityBoardLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityBoard board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}