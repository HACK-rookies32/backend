package com.example.kidhackbackend.global.notification.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 푸시 토큰 저장.
 *
 * <p><b>기기(토큰)당 한 행.</b> 한 계정이 여러 기기를 쓸 수 있으므로 {@code memberId} 는
 * 유일하지 않다. 대신 {@code token} 이 유일하다 — 같은 FCM 토큰이 여러 계정에 물리면
 * 기기 주인이 로그아웃/로그인한 뒤에도 이전 사용자 알림이 그 기기로 가기 때문이다
 * (등록 시 {@code findByToken} 으로 주인만 갱신한다).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "device_token",
        indexes = @Index(name = "idx_device_token_member", columnList = "memberId")
)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    public DeviceToken(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
    }

    /** 같은 기기가 다른 계정으로 로그인했을 때 이 토큰의 주인을 바꾼다. */
    public void assignTo(Long memberId) {
        this.memberId = memberId;
    }
}
