package com.example.kidhackbackend.mode;

import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.TreeSet;

/**
 * 실행 모드 분리 테스트의 공용 헬퍼.
 *
 * <p>현재 컨텍스트에 실제로 등록된 {@code /api/**} 핸들러 경로를 모아 준다.
 * {@code app.mode} 에 따라 {@link com.example.kidhackbackend.global.config.AppModeWebConfig}
 * 가 등록을 걸러 냈는지를 이 집합으로 검증한다.
 */
final class EndpointSplitSupport {

    private EndpointSplitSupport() {
    }

    static final String ADMIN_PREFIX = "/api/admin";

    /** 컨텍스트에 등록된 핸들러 중 {@code /api/} 로 시작하는 매핑 경로 전체. */
    static Set<String> mappedApiPaths(RequestMappingHandlerMapping mapping) {
        Set<String> paths = new TreeSet<>();
        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            PathPatternsRequestCondition condition = info.getPathPatternsCondition();
            if (condition == null) {
                continue;
            }
            for (String pattern : condition.getPatternValues()) {
                if (pattern.startsWith("/api/")) {
                    paths.add(pattern);
                }
            }
        }
        return paths;
    }
}
