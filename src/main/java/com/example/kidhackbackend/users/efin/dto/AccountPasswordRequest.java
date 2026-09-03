package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 계좌비밀번호 최초 설정 요청. 6자리 숫자. */
@Schema(description = "계좌비밀번호 설정 요청")
public record AccountPasswordRequest(
        @Schema(description = "(필수) 6자리 숫자 계좌비밀번호", example = "135790", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "계좌비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "계좌비밀번호는 6자리 숫자여야 합니다.") String password) {
}