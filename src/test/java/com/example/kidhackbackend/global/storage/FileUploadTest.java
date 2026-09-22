package com.example.kidhackbackend.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * multipart 업로드 (`POST /api/files/{category}`).
 *
 * <p>presign 은 S3 모드에서만 동작하고 로컬 디스크 모드에서는 409 를 주면서 "multipart 를
 * 쓰라"고 안내하는데, 그 multipart 가 커뮤니티·문의에만 있어 <b>공지 첨부는 로컬에서 올릴
 * 방법이 없었다.</b> 이 API 가 그 구멍을 메운다.
 *
 * <p>검증 대상은 세 가지다 — 분류별 권한, 서버가 만드는 저장 파일명, 응답 경로.
 */
class FileUploadTest {

    private FakeObjectStorage storage;
    private FileController controller;

    @BeforeEach
    void setUp() {
        storage = new FakeObjectStorage();
        // inquiry·profile 은 DB(BLOB) 저장으로 갈라진다. 여기서는 경로 형태만 확인하므로 store 를 스텁한다.
        DbFileStorageService dbFileStorageService = mock(DbFileStorageService.class);
        when(dbFileStorageService.store(any(), any())).thenAnswer(inv ->
                "/api/files/" + ((UploadCategory) inv.getArgument(0)).segment() + "/1");
        controller = new FileController(storage, new StorageProperties(), dbFileStorageService);
    }

    private Member member(Role role) {
        Member m = new Member();
        m.setRole(role);
        return m;
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "image/png", "bytes".getBytes());
    }

    private String urlOf(ApiResponse<Map<String, String>> response) {
        return response.getData().get("url");
    }

    @Test
    void 관리자는_공지_첨부를_올릴_수_있다() {
        var response = controller.upload(member(Role.SUPER_ADMIN), "notice", file("안내문.png"));

        String url = urlOf(response.getBody());
        assertThat(url).startsWith("/api/files/notice/").endsWith("_안내문.png");
        // 공지 저장 때 AttachmentValidator 가 실물 존재를 확인하므로 실제로 저장돼야 한다.
        assertThat(StoredObject.parse(url)).isPresent();
    }

    /** 저장 이름을 클라이언트가 정하면 `../` 로 다른 경로의 객체를 덮어쓸 수 있다. */
    @Test
    void 저장_파일명은_서버가_만든다() {
        var response = controller.upload(member(Role.SUPER_ADMIN), "notice", file("../../etc/passwd"));

        String url = urlOf(response.getBody());
        assertThat(url).doesNotContain("..").doesNotContain("etc/passwd");
        assertThat(url).startsWith("/api/files/notice/");
    }

    @Test
    void 부모는_커뮤니티_문의에_올릴_수_있다() {
        assertThat(urlOf(controller.upload(member(Role.PARENT), "community", file("a.png")).getBody()))
                .startsWith("/api/files/community/");
        assertThat(urlOf(controller.upload(member(Role.PARENT), "inquiry", file("b.png")).getBody()))
                .startsWith("/api/files/inquiry/");
    }

    /** 남의 도메인 네임스페이스에 올리지 못하게 막는다 — 공지는 관리자만. */
    @Test
    void 부모는_공지에_올릴_수_없다() {
        assertThatThrownBy(() -> controller.upload(member(Role.PARENT), "notice", file("a.png")))
                .isInstanceOf(AdminAccessDeniedException.class);
    }

    @Test
    void 자녀는_캡처에만_올릴_수_있다() {
        assertThat(urlOf(controller.upload(member(Role.CHILD), "screenshot", file("s.png")).getBody()))
                .startsWith("/api/files/screenshot/");
        assertThatThrownBy(() -> controller.upload(member(Role.CHILD), "community", file("s.png")))
                .isInstanceOf(AdminAccessDeniedException.class);
    }

    @Test
    void 로그인하지_않으면_거부한다() {
        assertThatThrownBy(() -> controller.upload(null, "notice", file("a.png")))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void 없는_분류는_404() {
        assertThatThrownBy(() -> controller.upload(member(Role.SUPER_ADMIN), "unknown", file("a.png")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 빈_파일은_거부한다() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> controller.upload(member(Role.SUPER_ADMIN), "notice", empty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
