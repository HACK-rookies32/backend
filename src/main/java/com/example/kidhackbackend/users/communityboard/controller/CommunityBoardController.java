package com.example.kidhackbackend.users.communityboard.controller;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.communityboard.dto.*;
import com.example.kidhackbackend.users.communityboard.service.CommunityBoardService;
import com.example.kidhackbackend.users.communityboard.service.FileStorageService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "20. 커뮤니티 게시판", description = "부모 커뮤니티 게시글/댓글/좋아요/첨부 — PARENT 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityBoardController {

    private final CommunityBoardService communityBoardService;
    private final FileStorageService fileStorageService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new InvalidCredentialException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new AdminAccessDeniedException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    @Operation(summary = "게시글 목록/검색",
            description = """
                    커뮤니티 게시글을 **최신순**으로 페이징 조회한다.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `keyword` | — | **제목 또는 작성자 닉네임** 부분 검색(대소문자 무시). 없으면 전체 |
                    | `page` / `size` | — | 페이징(기본 0 / 20) |
                    """)
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostPageResponse>> getPosts(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "제목·작성자 닉네임 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.getPosts(keyword, page, size)));
    }

    @Operation(summary = "게시글 작성",
            description = """
                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `title` | ✅ | 제목 |
                    | `content` | ✅ | 본문 |
                    | `attachments` | — | 첨부 공개 경로 목록. 먼저 `POST /api/community/attachments` 로 업로드해 받은 `url` 을 넣는다 |
                    | `links` | — | 관련 링크 목록 |
                    """)
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal Member member,
            @RequestBody PostRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.createPost(member, request)));
    }

    @Operation(summary = "게시글 상세",
            description = "본문·첨부·링크·좋아요 수 + **댓글 목록(오래된 순)** 을 함께 반환한다. 없는 postId → 404.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.getPostDetail(postId)));
    }

    @Operation(summary = "게시글 수정",
            description = "포함된(non-null) 필드만 반영. 없는 postId → 404. "
                    + "⚠️ [VULN-11] 작성자 미검증(IDOR) — postId 만 알면 남의 글도 수정 가능.")
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody PostRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.updatePost(postId, member, request)));
    }

    @Operation(summary = "게시글 삭제",
            description = "게시글과 첨부 실물을 함께 삭제한다. 없는 postId → 404. "
                    + "⚠️ [VULN-11] 작성자 미검증(IDOR) — postId 만 알면 남의 글도 삭제 가능(첨부 파일까지).")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        requireParent(member);
        communityBoardService.deletePost(postId, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "댓글 작성",
            description = """
                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `content` | ✅ | 댓글 내용 |

                    없는 postId → 404.
                    """)
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                communityBoardService.createComment(postId, member, request)));
    }

    @Operation(summary = "댓글 삭제",
            description = "**본인 댓글만** 삭제할 수 있다. 이 게시글의 댓글이 아니거나 없으면 404, 남의 댓글이면 403.")
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId) {
        requireParent(member);
        communityBoardService.deleteComment(postId, commentId, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "좋아요 토글",
            description = "누른 상태면 취소, 안 누른 상태면 좋아요. 토글 후 상태(`liked`)와 총 좋아요 수를 반환한다.")
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.toggleLike(postId, member)));
    }

    @Operation(summary = "첨부파일 업로드",
            description = """
                    `multipart/form-data` 로 파일 하나를 업로드하고 공개 경로(`url`)를 받는다.
                    이 `url` 을 게시글 작성/수정의 `attachments` 배열에 넣는다.

                    저장 흐름: WAS 가 파일을 받아 `ObjectStorage`(운영 S3 / 로컬 디스크)에 저장 →
                    `/api/files/community/{filename}` 경로 반환 → 조회 시 presigned S3 GET 으로 302 리다이렉트.

                    ⚠️ [VULN-10] 확장자 화이트리스트 없음 + Content-Type 을 클라이언트 값 그대로 사용(text/html 렌더 유도 가능) + 파일명 미새니타이즈.
                    """)
    @PostMapping(value = "/attachments", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AttachmentUploadResponse>> uploadAttachment(
            @AuthenticationPrincipal Member member,
            @RequestParam("file") MultipartFile file) {
        requireParent(member);
        String url = fileStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success(new AttachmentUploadResponse(url)));
    }
}
