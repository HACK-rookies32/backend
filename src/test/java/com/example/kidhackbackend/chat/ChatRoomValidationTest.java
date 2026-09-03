package com.example.kidhackbackend.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.kidhackbackend.global.exception.GlobalExceptionHandler;
import com.example.kidhackbackend.users.chat.controller.ChatRoomController;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import com.example.kidhackbackend.users.chat.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * QA #39 — POST /api/chat/rooms 의 childId 누락/null 은 404 가 아니라 400 이어야 한다.
 *
 * <p>예전엔 @NotNull·@Valid 가 없어 null childId 가 서비스까지 흘러가 "자녀 없음" 404 로 나갔다.
 */
class ChatRoomValidationTest {

    private MockMvc mvc;
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = Mockito.mock(ChatRoomService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new ChatRoomController(
                        chatRoomService, Mockito.mock(ChatMessageService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void childId_누락이면_400이고_방을_만들지_않는다() throws Exception {
        mvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));

        Mockito.verify(chatRoomService, Mockito.never()).getOrCreateRoom(any(), any());
    }

    @Test
    void childId_null이면_400이다() throws Exception {
        mvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"childId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));

        Mockito.verify(chatRoomService, Mockito.never()).getOrCreateRoom(any(), any());
    }
}
