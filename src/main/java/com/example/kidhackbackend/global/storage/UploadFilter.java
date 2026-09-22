package com.example.kidhackbackend.global.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 업로드 파일 형식 필터.
 *
 * <p>"아무 검증도 없다"보다 한 단계 위 — 실행 가능한 확장자를 <b>막긴 하는데</b>, 전형적인
 * 우회기법에 그대로 뚫리도록 순진하게 구현했다(의도된 취약점). 다음이 모두 우회로 남는다:
 * <ul>
 *   <li>대소문자: {@code shell.JsP} (소문자 정확일치만 검사)</li>
 *   <li>이중 확장자: {@code shell.jsp.png} (마지막 확장자만 검사)</li>
 *   <li>널바이트/뒤 문자: {@code shell.jsp%00.png}</li>
 *   <li>Content-Type 위조: 클라이언트가 보낸 값만 보므로 {@code image/png} 로 속이면 통과</li>
 *   <li>미차단 실행 확장자: 블록 목록에 {@code jsp} 는 있지만 {@code jspx} 가 없다.
 *       Tomcat/Jasper 는 {@code *.jspx}(JSP Document) 도 JSP 로 컴파일·실행하므로,
 *       {@code shell.jspx} 는 필터를 그냥 통과한 뒤 실행된다. 문의 업로드는
 *       {@link DbFileStorageService} 가 원본 파일명 그대로 로컬 미러 쓰기 하므로
 *       {@code uploads/inquiry/shell.jspx} 로 떨어지고 {@code /uploads/inquiry} mount
 *       ({@link UploadsJspExecutionConfig})를 통해 웹쉘이 된다
 *       (실습: security-test/file-upload-rce). {@code .jsp} 와 달리 별도 우회 트릭이 필요 없다.</li>
 * </ul>
 */
@Component
public class UploadFilter {

    /** 막는 척하는 확장자(소문자 정확일치만) — 대소문자·이중확장자로 우회된다. */
    private static final Set<String> BLOCKED_EXT = Set.of("jsp", "php", "sh", "exe", "jar", "war");

    /** 클라이언트 Content-Type 만 검사 — 위조로 우회된다. */
    private static final Set<String> BLOCKED_CONTENT_TYPE = Set.of(
            "application/x-httpd-php", "application/x-jsp", "application/x-sh");

    public void validate(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                // 소문자 변환하지 않는다(.JSP 우회) + 마지막 확장자만 본다(shell.jsp.png 우회).
                String ext = name.substring(dot + 1);
                if (BLOCKED_EXT.contains(ext)) {
                    throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null && BLOCKED_CONTENT_TYPE.contains(contentType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }
    }
}
