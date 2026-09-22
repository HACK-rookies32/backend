package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 커뮤니티 목록 썸네일을 백엔드가 대신 받아 오는 프록시.
 *
 * <p>흐름: 게시글 첫 첨부의 공개 경로({@code /api/files/community/...})를
 * {@link #toThumbnailUrl(String)} 가 presigned S3 URL 로 바꿔 프록시 주소
 * ({@code /api/community/thumbnail?url=<인코딩>}) 로 감싼다. 앱(Coil)은 그 프록시
 * 주소를 무인증으로 부르고, 컨트롤러가 {@link #fetch(String)} 로 실제 바이트를
 * 대신 받아 돌려준다.
 *
 * <p><b>[VULN-SSRF]</b> {@code fetch} 는 사용자가 넘긴 {@code url} 을 아무 검증 없이
 * 그대로 요청한다 — 스킴/호스트 allowlist, 사설·링크로컬 IP 차단, 리다이렉트 제한이
 * 전부 없다. 따라서 {@code ?url=} 을 서버만 닿는 내부 리소스(예: 클라우드 메타데이터
 * {@code http://169.254.169.254/latest/meta-data/...})로 바꾸면 서버를 경유해 그 응답을
 * 그대로 받아 볼 수 있다. 방어를 넣지 않는 것이 이 데모의 목적이다.
 */
@Service
@RequiredArgsConstructor
public class CommunityThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(CommunityThumbnailService.class);

    /** 프록시 엔드포인트 경로. toThumbnailUrl 이 만드는 주소와 컨트롤러 매핑이 같은 값을 써야 한다. */
    private static final String PROXY_PREFIX = "/api/community/thumbnail?url=";

    private final ObjectStorage objectStorage;

    /** 커넥션·응답을 5초로 끊는다. 데모 편의를 위한 값일 뿐 SSRF 방어와는 무관하다. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** fetch 결과. 응답을 가공하지 않고 status/body/Content-Type 을 그대로 실어 나른다. */
    public record ProxyResult(int status, byte[] body, String contentType) {}

    /**
     * 첨부 공개 경로를 프록시 썸네일 주소로 바꾼다.
     *
     * <p>{@link StoredObject#parse(String)} 로 (분류, 파일명) 을 뽑고
     * {@link ObjectStorage#downloadUrl(com.example.kidhackbackend.global.storage.UploadCategory, String)}
     * 으로 presigned URL 을 발급한 뒤, 그 URL 을 URL 인코딩해 프록시 주소에 담는다.
     * presigned 가 비어 있으면(로컬 저장소) 또는 경로를 해석할 수 없으면 원본 경로를
     * 그대로 돌려준다 — 로컬에서는 기존 {@code /api/files} 스트리밍으로 그림이 계속 뜬다.
     * (상대 경로를 프록시로 감싸면 {@link #fetch}의 HttpClient 가 절대 URI 로 풀지 못한다.)
     */
    public String toThumbnailUrl(String publicPath) {
        return StoredObject.parse(publicPath)
                .flatMap(o -> objectStorage.downloadUrl(o.category(), o.filename()))
                .map(uri -> PROXY_PREFIX + URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8))
                .orElse(publicPath);
    }

    /** 하위호환: 기존 GET 전용 호출부를 위한 오버로드. */
    public ProxyResult fetch(String url) {
        return fetch(url, null, null);
    }

    /**
     * 사용자가 넘긴 URL 을 서버가 대신 받아 온다.
     *
     * <p><b>[VULN-SSRF]</b> url·method·headers 어느 것도 검증하지 않는다. 받은 응답은
     * status·body·Content-Type 을 그대로 통과시킨다(필터링 없음). connect-refused 는 502,
     * timeout 은 504 로만 나누는데 이는 데모에서 결과를 읽기 쉽게 하려는 것뿐이다.
     *
     * <p>method 와 headers 를 그대로 받는다는 점이 특히 위험하다. 이 둘이 열려 있으면
     * SSRF 하나로 IMDSv2 의 2단계 흐름(① {@code PUT /latest/api/token} +
     * {@code X-aws-ec2-metadata-token-ttl-seconds} 헤더로 토큰 발급 →
     * ② {@code X-aws-ec2-metadata-token} 헤더를 실은 GET 으로 자격증명 조회)까지
     * 서버를 대신 시켜 클라우드 자격증명을 탈취할 수 있다. 방어를 넣지 않는 것이 목적이다.
     *
     * @param method  HTTP 메서드(null/blank 면 GET). 본문은 싣지 않는다.
     * @param headers "Name:Value" 문자열 목록. 그대로 요청 헤더로 주입한다.
     */
    public ProxyResult fetch(String url, String method, List<String> headers) {
        String httpMethod = (method == null || method.isBlank()) ? "GET" : method.trim().toUpperCase();
        // [VULN-SSRF] 서버가 사용자 지정 메서드·헤더로 임의 URL 에 나간다는 사실을 로그로 남긴다.
        log.warn("[VULN-SSRF] community thumbnail proxy fetching user-supplied request: method={} url={} headers={}",
                httpMethod, url, headers);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .method(httpMethod, HttpRequest.BodyPublishers.noBody());
            if (headers != null) {
                for (String raw : headers) {
                    if (raw == null) continue;
                    int sep = raw.indexOf(':');
                    if (sep > 0) {
                        // 검증 없이 이름:값 그대로 주입([VULN-SSRF]).
                        builder.header(raw.substring(0, sep).trim(), raw.substring(sep + 1).trim());
                    }
                }
            }
            HttpResponse<byte[]> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("application/octet-stream");
            return new ProxyResult(response.statusCode(), response.body(), contentType);
        } catch (ConnectException e) {
            return new ProxyResult(502, ("connect refused: " + e.getMessage())
                    .getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
        } catch (HttpTimeoutException e) {
            return new ProxyResult(504, ("timeout: " + e.getMessage())
                    .getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
        } catch (Exception e) {
            // 그 밖의 오류(잘못된 URL, IO 등)도 데모가 멈추지 않게 502 로 감싼다.
            return new ProxyResult(502, ("fetch failed: " + e.getMessage())
                    .getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8");
        }
    }
}
