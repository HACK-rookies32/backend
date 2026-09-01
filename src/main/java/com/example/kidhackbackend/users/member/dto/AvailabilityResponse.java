package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 가입 전 중복 확인 결과.
 *
 * <p>물어보지 않은 항목은 {@code null} 이다. 자녀는 이메일 없이 가입하므로 아이디만
 * 확인하는 호출이 흔하다 — 그때 이메일을 {@code true} 로 주면 "비어 있어도 쓸 수 있다"는
 * 뜻인지 "확인하지 않았다"는 뜻인지 구분되지 않는다.
 */
@Schema(description = "가입 전 중복 확인 결과 — 확인하지 않은 항목은 null")
public record AvailabilityResponse(
        @Schema(description = "이 아이디를 쓸 수 있는지. 확인하지 않았으면 null", example = "false", nullable = true)
        Boolean loginIdAvailable,

        @Schema(description = "이 이메일을 쓸 수 있는지. 확인하지 않았으면 null", example = "true", nullable = true)
        Boolean emailAvailable
) { }
