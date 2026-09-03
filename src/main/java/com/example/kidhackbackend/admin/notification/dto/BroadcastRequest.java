package com.example.kidhackbackend.admin.notification.dto;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 관리자 전체 알림 발송 요청. 공지 작성과는 별개다.
 */
@Schema(description = "전체 알림 발송 요청")
@Getter
public class BroadcastRequest {

    @Schema(description = "알림 유형(단말이 아이콘·라우팅에 사용): NOTICE / MAINTENANCE / UPDATE / EVENT 등", example = "NOTICE")
    @NotNull(message = "알림 유형은 필수입니다.")
    private NotificationType type;

    @Schema(description = "알림 제목(최대 255자)")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
    private String title;

    @Schema(description = "알림 본문(알림함에도 남음, 최대 255자)")
    @NotBlank(message = "본문은 필수입니다.")
    @Size(max = 255, message = "본문은 255자를 넘을 수 없습니다.")
    private String body;
}
