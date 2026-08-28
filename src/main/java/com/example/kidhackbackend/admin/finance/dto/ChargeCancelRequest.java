package com.example.kidhackbackend.admin.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 충전 취소 요청 (#139, 프론트 3차 1번).
 *
 * <p>취소 사유는 <b>필수</b>다. 감사로그 {@code description} 에 그대로 남아 "누가 · 언제 · 왜 돈을
 * 되돌렸는지" 를 완성한다. 사유 없는 자금 조작을 남기지 않으려는 것이라, 같은 자금 도메인인
 * {@code AdjustRequest} 가 사유 검증을 일부러 생략한 것({@code [VULN-ADMIN-2]})과는 반대 방향이다.
 */
@Schema(description = "충전 취소 요청")
public record ChargeCancelRequest(
        @Schema(description = "취소 사유. 감사 로그에 그대로 기록된다", example = "잘못 충전 민원 처리",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "취소 사유는 필수입니다.")
        String reason) {
}
