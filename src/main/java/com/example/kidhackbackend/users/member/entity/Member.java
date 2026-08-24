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
    private String username;

    @Column(nullable = false)
    private String password; // [VULN-1] 평문 저장

    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber; // [VULN-6] 형식 검증 없음, 암호화 없이 평문 저장

    private LocalDate birthDate;

    private String address; // [VULN-6] 형식 검증 없음

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Member parent;
}