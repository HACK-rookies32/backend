package com.example.kidhackbackend.global.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

/**
 * 공지 작성·수정 요청. 관리자 웹의 작성/수정 모달이 같은 본문을 쓴다.
 *
 * <p>수정(PATCH)에서 {@code null} 은 "그대로 두기" 다. 첨부·링크는 <b>빈 배열</b>을
 * 보내면 비운다 — null 과 구분해야 전부 지우는 조작이 가능하다.
 */
@Schema(description = "공지 작성/수정 요청 — 수정 시 null 은 미변경, 빈 배열은 비우기")
@Getter
public class NoticeRequest {

    // 생성 시 필수는 서비스에서 검증한다 — 수정(PATCH)은 null=미변경이라 여기에 @NotBlank 를 달면
    // 부분 수정이 깨진다. @Size 는 null 을 통과시키므로 양쪽에 안전하다(길이초과 500 방지, #38).
    @Schema(description = "제목(최대 255자)", example = "8월 정기 점검 안내",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "제목은 255자를 넘을 수 없습니다.")
    private String title;

    @Schema(description = "본문", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "첨부 공개 경로 목록. 관리자 웹이 `/api/files/notice/presign` → `/api/files/notice/complete` "
            + "로 업로드한 뒤 받은 URL 을 넣는다. 최대 10개", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 10, message = "첨부는 최대 10개입니다.")
    private List<String> attachments;

    @Schema(description = "관련 링크 목록. 최대 20개", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 20, message = "링크는 최대 20개입니다.")
    private List<String> links;
}
