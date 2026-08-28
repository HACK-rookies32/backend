package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.member.repository.AdminWalletProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 회원 지갑 목록의 한 행 (프론트 3차 2번).
 *
 * <p>「회원 지갑」 탭은 조회 화면이 아니라 <b>잔액 조정({@code POST /finance/users/{userId}/adjust})의 진입점</b>이다.
 * 목록이 없으면 조정 대상을 고를 화면 자체가 없다. 거래 원장({@code GET /finance/transactions})으로는
 * 회원별 <b>현재 잔액</b>을 알 수 없어 대체할 수 없다.
 */
@Schema(description = "관리자 회원 지갑 1행")
public record AdminWalletRow(
        @Schema(description = "회원 id", example = "3") Long userId,
        @Schema(description = "회원 실명", example = "김보호") String userName,
        @Schema(description = "회원 구분", example = "PARENT",
                allowableValues = {"PARENT", "CHILD"}) String role,
        @Schema(description = "현재 잔액(원). 지갑이 아직 없으면 0", example = "35000") long balance,
        @Schema(description = "지갑 최근 변동 시각. 지갑이 없으면 null") LocalDateTime updatedAt) {

    public static AdminWalletRow from(AdminWalletProjection p) {
        return new AdminWalletRow(
                p.getUserId(), p.getUserName(), p.getRole(), p.getBalance(), p.getUpdatedAt());
    }
}
