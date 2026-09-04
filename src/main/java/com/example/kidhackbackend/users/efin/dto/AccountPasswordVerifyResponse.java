package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 계좌비밀번호 확인 결과. */
@Schema(description = "계좌비밀번호 확인 결과")
public record AccountPasswordVerifyResponse(
        @Schema(description = "일치 여부", example = "true") boolean valid) {
}
