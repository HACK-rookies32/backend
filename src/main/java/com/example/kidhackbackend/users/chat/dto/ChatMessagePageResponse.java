package com.example.kidhackbackend.users.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅 메시지 커서 페이지 (최신순)")
@Getter
@AllArgsConstructor
public class ChatMessagePageResponse {

    @Schema(description = "메시지 목록 (최신 → 과거 순)")
    private List<ChatMessageResponse> messages;

    @Schema(description = "더 과거 메시지가 남아 있는지", example = "true")
    private boolean hasMore;

    @Schema(description = "이 페이지에서 가장 오래된 메시지의 시각. 다음 페이지 요청 시 `before` 파라미터로 그대로 넣는다. (비어 있으면 null)")
    private LocalDateTime oldestSentAt;
}
