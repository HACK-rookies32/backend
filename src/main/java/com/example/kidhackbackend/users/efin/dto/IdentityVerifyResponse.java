package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 실명확인 결과.
 *
 * <p>⚠️ [VULN-EFIN-7] 진위확인 판정({@code verified})을 그대로 응답에 돌려준다. 진위확인이 mock 이라
 * 정해진 규칙만 맞으면 통과하고, 이후 접근매체 발급에서 이 결과를 신뢰한다.
 */
@Schema(description = "실명확인 결과")
public record IdentityVerifyResponse(
        @Schema(description = "실명확인 통과 여부", example = "true") boolean verified,
        @Schema(description = "확인된 이름", example = "홍길동") String name,
        @Schema(description = "확인 시각") LocalDateTime verifiedAt) {
}