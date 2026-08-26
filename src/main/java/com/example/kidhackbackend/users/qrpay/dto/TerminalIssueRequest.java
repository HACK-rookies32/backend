package com.example.kidhackbackend.users.qrpay.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 매장 단말 등록/토큰 발급 요청.
 *
 * @param storeName 가맹점(매장) 이름
 */
public record TerminalIssueRequest(
        @NotBlank(message = "가맹점 이름은 필수입니다.") String storeName) {
}
