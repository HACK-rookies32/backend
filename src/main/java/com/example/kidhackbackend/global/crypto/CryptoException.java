package com.example.kidhackbackend.global.crypto;

/**
 * 복호 실패·형식 오류·재전송(nonce/timestamp) 위반 등 카드 암호 처리 실패.
 *
 * <p>규약 확정본: 구체 원인을 노출하지 않고 400 단일 코드로 응답한다(원인 노출 금지).
 * 저장 필드 복호 실패처럼 서버 내부 문제일 때도 이 예외를 쓴다.
 */
public class CryptoException extends RuntimeException {
    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
