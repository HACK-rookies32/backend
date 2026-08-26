package com.example.kidhackbackend.users.member.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "member")
@Getter
@Setter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId; // login용 id

    @Column(nullable = false)
    private String username; // 실제 이름

    @Column(nullable = false)
    private String password; // [VULN-1] 평문 저장

    private String nickname;

    @Column(unique = true) // nullable=false 제거 — CHILD는 이메일 없이 가입 가능
    private String email; // [VULN-6] 형식 검증 없음, 암호화 없이 평문 저장

    @Column(nullable = false)
    private String phoneNumber; // [VULN-6] 형식 검증 없음, 암호화 없이 평문 저장

    @Column(nullable = false)
    private LocalDate birthDate;

    private String address; // [VULN-6] 형식 검증 없음

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Member parent;

    // 기존 Member 엔티티에 아래 필드만 추가

    @Column(nullable = false)
    private boolean isDeleted = false;

    public void updateProfile(String nickname, String email, String phoneNumber, String address) {
        if (nickname != null) this.nickname = nickname;
        if (email != null) this.email = email;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (address != null) this.address = address;
    }
}