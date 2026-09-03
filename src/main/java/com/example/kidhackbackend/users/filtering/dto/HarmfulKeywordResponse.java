package com.example.kidhackbackend.users.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 지정 전역 유해 키워드 — 회원(부모) 읽기 전용 조회 응답.
 *
 * <p>부모 앱이 유해차단 화면에 <b>삭제 불가 섹션</b>으로 전역 키워드를 보여 주기 위한 목록이다.
 * 차단 목록 병합({@link BlockRuleResponse})과 달리 자녀별 규칙과 섞지 않고 관리자 키워드만 내려준다.
 *
 * <p>여기 {@code id}는 {@code harmful_keyword}의 <b>양수 raw id</b>다. 이 API는 삭제 경로
 * ({@code DELETE /rules/{id}})와 무관한 읽기 전용이라, {@link BlockRuleResponse#fromAdmin}이 쓰는
 * 음수 sentinel(오삭제 방지용)이 필요 없다.
 */
@Schema(description = "관리자 지정 전역 유해 키워드 (읽기 전용)")
public record HarmfulKeywordResponse(
        @Schema(description = "키워드 ID", example = "1") Long id,
        @Schema(description = "유해 키워드", example = "성인물") String keyword
) {
    public static HarmfulKeywordResponse from(HarmfulKeyword k) {
        return new HarmfulKeywordResponse(k.getId(), k.getKeyword());
    }
}
