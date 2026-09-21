package com.example.kidhackbackend.mode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code app.mode=admin} 이미지는 {@code /api/admin/**} 엔드포인트만 등록해야 한다.
 * 관리자 경로가 아닌 어떤 {@code /api} 엔드포인트도 이 이미지에 존재해선 안 된다.
 */
@SpringBootTest(properties = "app.mode=admin")
class AdminModeEndpointSplitTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void 관리자_모드는_api_admin_경로만_등록한다() {
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> apiPaths = EndpointSplitSupport.mappedApiPaths(mapping);

        assertThat(apiPaths).isNotEmpty();
        assertThat(apiPaths)
                .filteredOn(path -> !path.startsWith(EndpointSplitSupport.ADMIN_PREFIX))
                .as("관리자 이미지에 새어 든 비-admin 엔드포인트")
                .isEmpty();
    }

    @Test
    void 관리자_모드에는_모바일_신고_엔드포인트가_없다() {
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Set<String> apiPaths = EndpointSplitSupport.mappedApiPaths(mapping);

        // admin 폴더의 AdminCommunityController 에 있지만 경로가 /api/community 라 모바일에 속하는 신고 API.
        assertThat(apiPaths).doesNotContain(
                "/api/community/posts/{postId}/report",
                "/api/community/posts/{postId}/comments/{commentId}/report");
    }
}
