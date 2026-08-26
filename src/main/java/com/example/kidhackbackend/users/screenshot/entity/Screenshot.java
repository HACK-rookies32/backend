package com.example.kidhackbackend.users.screenshot.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 자녀 화면 캡처 메타데이터.
 *
 * <p>이미지 실물은 S3(UploadCategory.SCREENSHOT)에 있고, 여기에는 그 위치와 촬영
 * 맥락만 남는다. 부모가 목록을 조회할 때 이 행을 페이징하고 imagePath 로 다운로드
 * URL 을 발급한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "screenshot",
        indexes = @Index(name = "idx_screenshot_child", columnList = "childId,capturedAt")
)
public class Screenshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 캡처 대상 자녀. */
    @Column(nullable = false)
    private Long childId;

    /**
     * 공개 경로({@code /api/files/screenshot/{filename}}).
     *
     * <p>presigned S3 URL 이 아니라 우리 서버 경로다. 만료되지 않으므로 DB 에 남길 수
     * 있고, 부모가 조회할 때 이 경로가 다시 FileController 로 들어가 presigned GET 으로
     * 리다이렉트된다.
     */
    @Column(nullable = false)
    private String imagePath;

    /** 자녀 앱이 실제로 촬영한 시각(서버 수신 시각과 다를 수 있어 따로 받는다). */
    @Column(nullable = false)
    private LocalDateTime capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaptureTrigger trigger;

    public Screenshot(Long childId, String imagePath, LocalDateTime capturedAt, CaptureTrigger trigger) {
        this.childId = childId;
        this.imagePath = imagePath;
        this.capturedAt = capturedAt;
        this.trigger = trigger;
    }
}