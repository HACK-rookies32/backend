package com.example.kidhackbackend.admin.community.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_community_member_ban")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false)
    private String reason;

    // null 이면 밴 해제 상태
    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    public MemberBan(Long memberId) {
        this.memberId = memberId;
    }

    /** bannedUntil 이 null 이면 해제, 값이 있으면 해당 시각까지 밴. */
    public void apply(String reason, LocalDateTime bannedUntil) {
        this.reason = reason;
        this.bannedUntil = bannedUntil;
    }

    public boolean isBanned() {
        return bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now());
    }
}
