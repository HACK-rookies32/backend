package com.example.kidhackbackend.users.pairing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

@Schema(description = "페어링 연결 요청")
@Getter
public class PairingConnectRequest {

    @Schema(description = "(필수) 부모가 발급받은 6자리 페어링 코드", example = "840975",
            requiredMode = RequiredMode.REQUIRED)
    private String code;

    // [VULN-PAIRING-1] 레거시 정렬 파라미터. 값이 있으면 ORDER BY 절에 그대로 결합됨(ORDER BY SQLi).
    //   미지정 시 정상 연결하므로 코드만 보내는 정상 클라이언트는 영향 없음.
    @Schema(description = "(선택·레거시) 코드 정렬 기준. 값이 있으면 조회만 하고 연결하지 않으며, "
            + "ORDER BY 절에 그대로 결합됨 — SQL Injection 취약(의도적). 미지정 시 정상 연결.",
            example = "expires_at")
    private String sort;
}
