package com.example.kidhackbackend.mode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code app.mode=mobile} 이미지는 {@code /api/admin/**} 을 제외한 나머지 엔드포인트만 등록해야 한다.
 * 관리자 엔드포인트는 어떤 것도 이 이미지에 존재해선 안 된다.
 *
 * <p>{@link AdminModeEndpointSplitTest} 와 함께 보면, admin 이미지에는 admin 경로만·mobile
 * 이미지에는 비-admin 경로만 등록되므로 두 이미지의 엔드포인트 집합은 서로소(겹침 없음)임이 증명된다.
 */
@SpringBootTest(properties = "app.mode=mobile")
class MobileModeEndpointSplitTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void 모바일_모드는_api_admin_경로를_등록하지_않는다() {
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> apiPaths = EndpointSplitSupport.mappedApiPaths(mapping);

        assertThat(apiPaths).isNotEmpty();
        assertThat(apiPaths)
                .filteredOn(path -> path.startsWith(EndpointSplitSupport.ADMIN_PREFIX))
                .as("모바일 이미지에 새어 든 관리자 엔드포인트")
                .isEmpty();
    }

    @Test
    void 모바일_모드에는_대표_모바일_엔드포인트가_남는다() {
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> apiPaths = EndpointSplitSupport.mappedApiPaths(mapping);

        // 대표 모바일 엔드포인트 + admin 폴더에 있지만 경로상 모바일인 신고 API 가 등록되어 있어야 한다.
        assertThat(apiPaths).contains(
                "/api/auth/login",
                "/api/community/posts",
                "/api/community/posts/{postId}/report");
    }
}
