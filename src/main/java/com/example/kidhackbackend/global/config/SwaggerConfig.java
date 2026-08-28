package com.example.kidhackbackend.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAPI(Swagger) 공통 설정.
 *
 * <p>모든 응답은 {@code com.example.kidhackbackend.global.response.ApiResponse} 봉투
 * ({@code { success, data, error } })로 감싸 나간다. 에러 응답의 표준 형태(400/401/403/500)를
 * 전 오퍼레이션에 자동으로 붙여, 컨트롤러마다 {@code @ApiResponse} 를 손으로 달지 않아도
 * Swagger UI 에서 에러 규격이 보이도록 한다. (개별 컨트롤러가 직접 선언한 상태 코드는 덮어쓰지 않는다.)
 */
@OpenAPIDefinition(
        info = @Info(
                title = "kidhack-backend API",
                version = "v1",
                description = """
                        자녀보호앱 REST API 명세서.

                        - 모든 응답은 `{ "success": boolean, "data": ..., "error": { "code", "message" } | null }` 형태로 감싸진다.
                        - 인증이 필요한 API 는 우측 상단 **Authorize** 에 `Bearer {accessToken}` 을 넣고 호출한다.
                        - 아래 각 API 의 상세 에러 메시지·취약점 노트는 별도 명세서(Notion) 를 함께 참고.
                        """
        ),
        servers = {
                @Server(url = "https://kidhackapp.com", description = "운영 서버"),
                @Server(url = "http://localhost:8080", description = "로컬 개발")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
public class SwaggerConfig {

    /**
     * 모바일(회원) API 그룹 — {@code /api/admin/**} 를 제외한 전체.
     * Swagger UI 우측 상단 "Select a definition" 에서 전환한다. (/v3/api-docs/member)
     */
    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("member")
                .displayName("회원 (모바일 앱)")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**")
                .build();
    }

    /** 관리자(웹) API 그룹 — {@code /api/admin/**}. (/v3/api-docs/admin) */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("관리자 (웹)")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /** 전 오퍼레이션에 표준 에러 응답(400/401/403/500)을 추가한다. */
    @Bean
    public OperationCustomizer commonErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            putIfAbsent(responses, "400", "요청 값이 올바르지 않음 (검증 실패·파라미터 형식 오류 등)",
                    "INVALID_PARAMETER", "요청 값이 올바르지 않습니다.");
            putIfAbsent(responses, "401", "인증 실패 (토큰 없음·만료·유효하지 않음)",
                    "UNAUTHORIZED", "인증이 필요합니다.");
            putIfAbsent(responses, "403", "권한 없음 (역할이 맞지 않음)",
                    "FORBIDDEN", "접근 권한이 없습니다.");
            putIfAbsent(responses, "500", "서버 내부 오류",
                    "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
            return operation;
        };
    }

    private void putIfAbsent(ApiResponses responses, String status, String description,
                             String code, String message) {
        if (responses.containsKey(status)) {
            return;
        }
        responses.addApiResponse(status, errorResponse(description, code, message));
    }

    private ApiResponse errorResponse(String description, String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("data", null);
        body.put("error", error);

        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("success", new io.swagger.v3.oas.models.media.BooleanSchema().example(false));
        schema.addProperty("data", new ObjectSchema().nullable(true));
        schema.addProperty("error", new ObjectSchema()
                .addProperty("code", new StringSchema().example(code))
                .addProperty("message", new StringSchema().example(message)));

        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(schema)
                                .addExamples("default", new Example().value(body))));
    }
}
