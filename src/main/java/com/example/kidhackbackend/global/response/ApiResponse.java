package com.example.kidhackbackend.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "공통 응답 봉투 — 모든 API 응답이 이 형태로 감싸진다.")
@Getter
public class ApiResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "성공 시 응답 데이터 (실패 시 null)")
    private final T data;

    @Schema(description = "실패 시 에러 정보 (성공 시 null). 상세 형태는 400/401/403/500 응답 예시 참고.",
            nullable = true)
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}