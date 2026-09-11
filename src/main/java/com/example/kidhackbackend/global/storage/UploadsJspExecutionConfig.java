package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;

/**
 * [VULN-10] 1:1 문의 첨부 디렉터리를 JSP 실행 컨텍스트에 얹는 "실행 싱크".
 *
 * <p>파일 업로드 취약점의 마지막 조각이다. 지금까지의 흐름은:
 * <ol>
 *   <li>문의 첨부 업로드({@code POST /api/inquiries/upload})는 약한 {@link UploadFilter} 를
 *       거친다. {@code .jsp} 는 막히지만 blocklist 가 {@code .jspx}(JSP Document)를 빠뜨려
 *       {@code shell.jspx} 는 그냥 통과한다.</li>
 *   <li>{@link DbFileStorageService} 가 <b>원본 파일명 그대로</b> 로컬 미러 쓰기를 하므로
 *       {@code uploads/inquiry/shell.jspx} 로 디스크에 떨어진다.</li>
 *   <li>그런데 {@link FileController} 는 그 파일을 <b>바이트로만</b> 내려주므로 브라우저가
 *       소스를 볼 뿐 서버에서 실행되지 않았다 — RCE 가 "안 터지는" 이유가 이것이다.</li>
 * </ol>
 *
 * <p>여기서 디스크의 {@code uploads/inquiry} 디렉터리를 임베디드 Tomcat 웹 컨텍스트의
 * {@code /uploads/inquiry} 경로에 <b>웹 리소스로 mount</b> 한다. build.gradle 의
 * {@code tomcat-embed-jasper} 덕분에 Spring Boot 가 {@code *.jspx} 를 JspServlet 에
 * 매핑해 두었으므로, 이제 {@code GET /uploads/inquiry/shell.jspx} 요청이 디스크의 JSP 를
 * 런타임 컴파일·실행한다 → <b>웹쉘 RCE 완성</b>.
 *
 * <p>범위를 <b>문의 디렉터리 하나로</b> 한정한다. 다른 분류(커뮤니티·프로필 등)는 mount 하지
 * 않으므로 그쪽으로는 실행되지 않는다.
 *
 * <p>확장자 매핑({@code *.jspx})은 서블릿 컨테이너 레벨에서 DispatcherServlet({@code /})
 * 보다 우선하므로, {@code /uploads/inquiry/**} 는 Spring MVC 를 거치지 않고 곧장 JSP 엔진으로
 * 들어간다. SecurityConfig 가 {@code permitAll} 이라 인증 없이도 접근된다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UploadsJspExecutionConfig {

    private final StorageProperties props;

    /** {@code uploads/inquiry} 디렉터리를 {@code /uploads/inquiry} 웹 경로에 얹어 그 아래 JSP 가 실행되게 한다. */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> uploadsJspResources() {
        return factory -> factory.addContextCustomizers(context -> {
            String segment = UploadCategory.INQUIRY.segment();
            File base = Paths.get(props.getPrefix(), segment).toAbsolutePath().normalize().toFile();
            // 서버 기동 시점엔 아직 업로드가 없어 디렉터리가 없을 수 있다. 없으면 만들어
            // 둬야 Tomcat 이 리소스셋을 붙일 수 있다.
            if (!base.exists() && !base.mkdirs()) {
                log.warn("uploads/{} 디렉터리를 만들 수 없어 JSP 실행 mount 를 건너뜁니다: {}", segment, base);
                return;
            }
            // WebResourceRoot 는 컨텍스트 START 직전에 준비되므로 그 타이밍에 붙인다.
            context.addLifecycleListener(event -> {
                if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
                    WebResourceRoot resources = context.getResources();
                    if (resources == null) {
                        resources = new StandardRoot(context);
                        context.setResources(resources);
                    }
                    // 웹 경로 /uploads/inquiry  ->  디스크 <cwd>/uploads/inquiry
                    resources.createWebResourceSet(
                            WebResourceRoot.ResourceSetType.POST,
                            "/uploads/" + segment,
                            base.getAbsolutePath(),
                            null,
                            "/");
                    log.info("[VULN-10] uploads JSP 실행 mount: /uploads/{} -> {}", segment, base);
                }
            });
        });
    }
}
