package com.example.kidhackbackend.notice;

import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.notification.controller.AdminNotificationController;
import com.example.kidhackbackend.admin.notification.service.BroadcastNotificationService;
import com.example.kidhackbackend.global.exception.GlobalExceptionHandler;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전체 알림 발송 요청 계약.
 *
 * <p>예전 본문은 {@code {message}} 하나였다. 관리자 웹은 {@code {type,title,body}} 를 보내므로
 * 그대로 두면 <b>message 가 null 인 채로 전 회원에게 빈 알림</b>이 나갔다. 검증이 없어
 * 400 도 나지 않았다. 그 자리를 이 테스트가 지킨다.
 */
class AdminNotificationControllerTest {

    private MockMvc mvc;
    private BroadcastNotificationService broadcastService;

    @BeforeEach
    void setUp() {
        broadcastService = Mockito.mock(BroadcastNotificationService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new AdminNotificationController(
                        broadcastService, Mockito.mock(AdminAuditLogService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 유형과_제목과_본문이_그대로_전달된다() throws Exception {
        mvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"MAINTENANCE","title":"점검 안내","body":"8/29 새벽 점검 예정입니다."}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        Mockito.verify(broadcastService)
                .broadcast(eq(NotificationType.MAINTENANCE), title.capture(), body.capture());

        assertThat(title.getValue()).isEqualTo("점검 안내");
        assertThat(body.getValue()).isEqualTo("8/29 새벽 점검 예정입니다.");
    }

    @Test
    void 본문이_비면_400_이고_발송되지_않는다() throws Exception {
        mvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"NOTICE","title":"제목","body":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));

        // 빈 알림이 전 회원에게 나가는 일이 없어야 한다.
        Mockito.verify(broadcastService, Mockito.never()).broadcast(any(), any(), any());
    }

    @Test
    void 유형이_빠지면_400_이다() throws Exception {
        mvc.perform(post("/api/admin/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","body":"본문"}"""))
                .andExpect(status().isBadRequest());

        Mockito.verify(broadcastService, Mockito.never()).broadcast(any(), any(), any());
    }
}