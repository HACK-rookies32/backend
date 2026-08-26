package com.example.kidhackbackend.users.wallet.dto;

/**
 * 이체/회수 응답 (No.39 · No.40). 보내는 쪽·받는 쪽의 잔액을 함께 담는다.
 *
 * @param fromOwnerId 보내는 지갑 소유자 id
 * @param toOwnerId   받는 지갑 소유자 id
 * @param amount      이체 금액(원)
 * @param fromBalance 이체 후 보내는 쪽 잔액
 * @param toBalance   이체 후 받는 쪽 잔액
 */
public record TransferResponse(
        Long fromOwnerId,
        Long toOwnerId,
        long amount,
        long fromBalance,
        long toBalance) {
}
