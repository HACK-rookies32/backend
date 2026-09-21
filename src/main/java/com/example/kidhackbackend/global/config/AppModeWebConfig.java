package com.example.kidhackbackend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * {@link AppMode} 에 따라 컨트롤러 핸들러의 URL 등록을 걸러 낸다.
 *
 * <p>Spring MVC 가 핸들러를 매핑하기 직전에 {@link RequestMappingHandlerMapping#getMappingForMethod}
 * 를 가로채, 해당 핸들러의 경로가 현재 모드에 속하지 않으면 {@code null} 을 돌려 <b>등록 자체를
 * 생략</b>한다. 빈은 그대로 살아 있고(서로 참조하는 서비스·엔티티 유지) URL 매핑만 사라지므로,
 * 다른 이미지에 속한 엔드포인트는 이 이미지에서 <b>존재하지 않는 것</b>(404)이 된다.
 *
 * <p>분할은 경로 접두사 {@code /api/admin} 하나로만 한다:
 * <ul>
 *   <li>{@code admin}  — {@code /api/admin} 으로 시작하는 경로만 등록</li>
 *   <li>{@code mobile} — {@code /api/admin} 으로 시작하지 <b>않는</b> 경로만 등록</li>
 *   <li>{@code all}    — 전부 등록(로컬/CI)</li>
 * </ul>
 * 두 모드의 등록 집합은 서로소이므로 어떤 엔드포인트도 두 이미지에 동시에 존재할 수 없다.
 *
 * <p>참고: {@code /api/**} 가 아닌 서블릿 레벨 경로({@code /actuator/**}, {@code /swagger-ui.html},
 * {@code /h2-console}, STOMP {@code /ws-chat}, JSP mount {@code /uploads/**})는 {@code @RequestMapping}
 * 핸들러가 아니라 이 필터의 대상이 아니다 — 두 이미지 모두에서 그대로 동작한다(헬스체크·문서 등 인프라 경로).
 */
@Slf4j
@Configuration
public class AppModeWebConfig {

    /** 관리자 엔드포인트를 가르는 유일한 경로 접두사. */
    static final String ADMIN_PREFIX = "/api/admin";

    private final AppMode mode;

    public AppModeWebConfig(@Value("${app.mode:all}") String mode) {
        this.mode = AppMode.from(mode);
        log.info("[APP_MODE] 실행 모드 = {} (admin: /api/admin/** 만 · mobile: 그 외만 · all: 전부)", this.mode);
    }

    @Bean
    public WebMvcRegistrations appModeWebMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new AppModeRequestMappingHandlerMapping(mode);
            }
        };
    }

    /**
     * 핸들러 경로가 현재 모드에 속할 때만 매핑을 반환하는 커스텀 매핑.
     * 모드가 {@link AppMode#ALL} 이면 원본을 그대로 통과시켜 로컬/CI 동작을 바꾸지 않는다.
     */
    static class AppModeRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

        private final AppMode mode;

        AppModeRequestMappingHandlerMapping(AppMode mode) {
            this.mode = mode;
        }

        @Override
        protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
            RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
            if (info == null || mode == AppMode.ALL) {
                return info;
            }
            boolean adminPath = isAdminPath(info);
            return switch (mode) {
                case ADMIN -> adminPath ? info : null;
                case MOBILE -> adminPath ? null : info;
                case ALL -> info; // 위에서 이미 반환됨. 컴파일러 완결성용.
            };
        }

        /** 매핑된 경로 중 하나라도 {@code /api/admin} 으로 시작하면 관리자 경로로 본다. */
        private boolean isAdminPath(RequestMappingInfo info) {
            for (String pattern : patternsOf(info)) {
                if (pattern != null && pattern.startsWith(ADMIN_PREFIX)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 경로 패턴 문자열을 꺼낸다. Spring Boot 4 는 PathPattern 이 기본이라
         * {@code getPathPatternsCondition()} 가 채워진다(이 앱은 AntPathMatcher 를 켜지 않는다).
         */
        private Set<String> patternsOf(RequestMappingInfo info) {
            PathPatternsRequestCondition pathPatterns = info.getPathPatternsCondition();
            return pathPatterns != null ? pathPatterns.getPatternValues() : Set.of();
        }
    }
}
