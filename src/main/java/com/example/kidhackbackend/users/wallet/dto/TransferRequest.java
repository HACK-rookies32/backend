package com.example.kidhackbackend.users.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 용돈 이체 / 잔액 회수 요청 (No.39 · No.40).
 *
 * @param childId 대상 자녀 회원 id
 * @param amount  이체/회수 금액(원), 1 이상
 */
public record TransferRequest(
        @NotNull(message = "childId는 필수입니다.") Long childId,
        @Positive(message = "금액은 1원 이상이어야 합니다.") long amount) {
}
