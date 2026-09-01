package com.example.kidhackbackend.users.member.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    /**
     * 탈퇴 여부(소프트 삭제).
     *
     * <p><b>정수로 매핑하는 이유(#173)</b>: 운영 Oracle 은 V1 에서 {@code number(1,0)} 로 만들어졌는데,
     * 로컬·CI 는 {@code ddl-auto: update} 가 이 필드를 보고 <b>BOOLEAN</b> 으로 만든다. 그래서 네이티브
     * 쿼리에서 {@code is_deleted = 0} 을 쓰면 운영은 통과하고 H2 는
     * "BOOLEAN 과 INTEGER 는 비교 불가" 로 터진다. 매핑을 정수로 고정해 두 환경의 컬럼 타입을 맞춘다.
     */
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean isDeleted = false;

    public void updateProfile(String nickname, String email, String phoneNumber, String address) {
        if (nickname != null) this.nickname = nickname;
        if (email != null) this.email = email;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (address != null) this.address = address;
    }
}