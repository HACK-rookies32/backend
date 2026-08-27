package com.example.kidhackbackend.screenshot;

import com.example.kidhackbackend.global.exception.GlobalExceptionHandler;
import com.example.kidhackbackend.users.screenshot.controller.ScreenshotController;
import com.example.kidhackbackend.users.screenshot.service.ScreenshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 잘못 보낸 요청이 명세서 3.4 대로 400 INVALID_PARAMETER 로 나가는지 본다.
 *
 * <p>이 엔드포인트는 필수 파라미터가 셋(file/capturedAt/trigger)이라 클라이언트가
 * 가장 잘 틀리는 자리다. GlobalExceptionHandler 가 변환 실패를 잡지 않으면
 * {@code Exception} 핸들러로 떨어져 500 INTERNAL_ERROR 가 나갔다.
 */
class ScreenshotControllerErrorTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .standaloneSetup(new ScreenshotController(Mockito.mock(ScreenshotService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 열거형에_없는_trigger_는_400() throws Exception {
        mvc.perform(multipart("/api/children/1/screenshots")
                        .file(image())
                        .param("capturedAt", "2026-08-26T10:00:00")
                        .param("trigger", "WHENEVER_I_WANT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void 형식이_어긋난_capturedAt_은_400() throws Exception {
        mvc.perform(multipart("/api/children/1/screenshots")
                        .file(image())
                        .param("capturedAt", "어제쯤")
                        .param("trigger", "SCHEDULED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void 필수_파라미터_누락은_400() throws Exception {
        mvc.perform(multipart("/api/children/1/screenshots")
                        .file(image())
                        .param("capturedAt", "2026-08-26T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void 파일_파트_누락은_400() throws Exception {
        mvc.perform(multipart("/api/children/1/screenshots")
                        .param("capturedAt", "2026-08-26T10:00:00")
                        .param("trigger", "SCHEDULED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    @Test
    void 목록_조회의_잘못된_기간도_400() throws Exception {
        mvc.perform(get("/api/children/1/screenshots").param("from", "지난주"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "capture.jpg", "image/jpeg", "fake-image".getBytes());
    }
}