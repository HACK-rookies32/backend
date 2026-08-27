package com.example.kidhackbackend.admin.notification.dto;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 관리자 전체 알림 발송 요청.
 *
 * <p>공지 작성과는 별개다. 공지를 써도 알림은 나가지 않고, 알림은 이 요청으로만 나간다.
 *
 * <p>예전에는 {@code message} 한 필드였는데, 관리자 웹은 유형과 제목을 따로 보낸다.
 * 그대로 두면 {@code message} 가 null 이 되어 <b>전 회원에게 빈 알림</b>이 나갔다.
 */
@Getter
public class BroadcastRequest {

    /** NOTICE / MAINTENANCE / UPDATE / EVENT. 단말이 아이콘·라우팅에 쓴다. */
    @NotNull(message = "알림 유형은 필수입니다.")
    private NotificationType type;

    /** 단말 알림창 제목. */
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    /** 단말 알림창 본문. 알림함에도 이 값이 남는다. */
    @NotBlank(message = "본문은 필수입니다.")
    private String body;
}