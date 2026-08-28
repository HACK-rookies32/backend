package com.example.kidhackbackend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "에러 정보")
@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    // example 을 두지 않는다 — springdoc 이 성공(200) 응답의 error(null) 자리에까지
    // 이 값을 채워 넣어 "성공인데 에러코드가 보이는" 오해를 만든다. 실제 에러 응답 예시는
    // SwaggerConfig 의 표준 400/401/403/500 예시에서 상태코드별로 정확히 제공한다.
    @Schema(description = "에러 코드")
    private final String code;

    @Schema(description = "에러 메시지")
    private final String message;
}