package com.example.kidhackbackend.users.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 차단 규칙 응답 (No.6).
 *
 * <p>자녀 앱이 이 목록으로 기기 안 차단 목록을 만든다. <b>부모가 등록한 규칙과 관리자가 등록한 전역
 * 키워드를 함께</b> 내려준다(QA #17). 예전엔 부모 규칙만 줘서 관리자 키워드가 자녀 기기 차단에
 * 반영되지 않았다.
 *
 * <p>{@code source} 로 둘을 구분한다. 관리자 키워드는 관리자 페이지에서만 관리하므로 앱에서 지울 수
 * 없어야 하는데, 삭제는 {@code id} 로 나간다({@code DELETE /rules/{id}}). 그래서 관리자 키워드에는
 * <b>음수 id({@code -(keywordId)})</b>를 준다 — {@code block_rule}(양수)과 절대 겹치지 않아, 앱이
 * 실수로 삭제를 시도해도 서버가 그 규칙을 못 찾아 조용히 무시된다(부모 규칙 오삭제 방지).
 */
@Schema(description = "차단 규칙")
public record BlockRuleResponse(
        @Schema(description = "규칙 ID. 관리자 키워드는 음수(-keywordId)", example = "1") Long id,
        @Schema(description = "자녀 회원 ID. 관리자(전역) 키워드는 null", example = "7") Long childId,
        @Schema(description = "차단 패턴", example = "badsite.com") String pattern,
        @Schema(description = "출처: PARENT(부모 등록, 삭제 가능) / ADMIN(관리자 전역, 앱에서 삭제 불가)",
                example = "PARENT") String source
) {
    /** 부모가 등록한 규칙. */
    public static BlockRuleResponse from(BlockRule r) {
        return new BlockRuleResponse(r.getId(), r.getChildId(), r.getPattern(), "PARENT");
    }

    /** 관리자가 등록한 전역 키워드. 음수 id 로 실어 앱에서 삭제되지 않게 한다. */
    public static BlockRuleResponse fromAdmin(HarmfulKeyword k) {
        return new BlockRuleResponse(-k.getId(), null, k.getKeyword(), "ADMIN");
    }
}
