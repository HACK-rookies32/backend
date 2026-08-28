package com.example.kidhackbackend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "에러 정보")
@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    @Schema(description = "에러 코드", example = "INVALID_PARAMETER")
    private final String code;

    @Schema(description = "에러 메시지", example = "요청 값이 올바르지 않습니다.")
    private final String message;
}