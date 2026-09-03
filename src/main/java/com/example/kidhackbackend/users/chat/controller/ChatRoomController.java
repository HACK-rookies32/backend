package com.example.kidhackbackend.users.chat.controller;

import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.chat.dto.ChatMessagePageResponse;
import com.example.kidhackbackend.users.chat.dto.ChatRoomRequest;
import com.example.kidhackbackend.users.chat.dto.ChatRoomResponse;
import com.example.kidhackbackend.users.chat.dto.ChatRoomSummaryResponse;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import com.example.kidhackbackend.users.chat.service.ChatRoomService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "22. 채팅", description = """
        채팅방 목록/생성 · 메시지 커서 페이징 · 읽음 처리.

        **실시간 송수신 (STOMP over WebSocket)**
        - 접속: `/ws-chat` (SockJS)
        - 구독: `/sub/chat/{roomId}`
        - 발행: `/pub/chat/{roomId}` (본문은 `ChatMessageRequest`)
        - CONNECT 헤더: `Authorization: Bearer <JWT>`
        - ⚠️ 소켓으로 수신하는 메시지 페이로드는 `ChatMessageResponse` **원본**이며, REST 처럼 `ApiResponse` 봉투로 감싸지 않는다.
        """)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    private Member requireLogin(Member member) {
        if (member == null) {
            throw new InvalidCredentialException("로그인이 필요합니다.");
        }
        return member;
    }

    @Operation(summary = "내 채팅방 목록",
            description = """
                    로그인한 회원이 부모 또는 자녀로 속한 모든 채팅방을 **최근 대화순**으로 반환한다.
                    자녀가 여러 명인 부모는 이 목록에서 방을 골라 들어간다.

                    각 항목: 상대방(닉네임·실명·프로필), 마지막 메시지와 시각, 내가 안 읽은 수.
                    """)
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> listRooms(
            @AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(chatRoomService.listRooms(member)));
    }

    @Operation(summary = "채팅방 생성 또는 조회",
            description = """
                    부모-자녀 조합의 방이 있으면 반환하고 없으면 생성한다. 로그인한 **PARENT** 만 호출할 수 있고,
                    `childId` 는 그 부모의 자녀여야 한다(아니면 404).

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID (부모는 토큰에서 식별) |
                    """)
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetRoom(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ChatRoomRequest request) {
        requireLogin(member);
        ChatRoom room = chatRoomService.getOrCreateRoom(member, request.getChildId());
        return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(room)));
    }

    @Operation(summary = "채팅방 메시지 목록 (커서 페이징)",
            description = """
                    메시지를 **최신 → 과거 순**으로 반환한다. 무한 스크롤은 응답의 `oldestSentAt` 을
                    다음 요청 `before` 로 넘겨 이어 받는다.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `before` | — | 이 시각(ISO-8601, 예 `2026-08-29T11:20:00`) 이전 메시지만. 없으면 최신부터 |
                    | `size` | — | 페이지 크기(기본 20, 최대 100) |

                    ⚠️ [VULN-CHAT-1] 요청자가 이 방의 부모/자녀인지 검증하지 않는다(IDOR) — roomId 만 알면 남의 대화도 조회 가능.
                    """)
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
            @Parameter(description = "채팅방 ID") @PathVariable String roomId,
            @Parameter(description = "이 시각 이전 메시지만 (ISO-8601)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                chatMessageService.getMessages(roomId, before, size)));
    }

    @Operation(summary = "채팅방 읽음 처리",
            description = "이 방의 메시지를 **지금 시점까지** 읽은 것으로 표시한다. 방 참여자(부모/자녀)만 호출 가능. "
                    + "이후 목록의 `unreadCount` 계산에 반영된다.")
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "채팅방 ID") @PathVariable String roomId) {
        requireLogin(member);
        chatRoomService.markRead(member, roomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
