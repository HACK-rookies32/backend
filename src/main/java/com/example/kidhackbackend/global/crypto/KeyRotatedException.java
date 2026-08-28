package com.example.kidhackbackend.global.crypto;

/**
 * 요청의 keyId가 현재 서버 키와 달라(회전됨) 복호할 수 없을 때.
 *
 * <p>규약 확정본: 복호 실패는 일반적으로 400 단일 코드지만, <b>키 회전 케이스만 분리</b>해
 * 프론트가 공개키를 재조회하도록 신호한다({@code KEY_ROTATED}).
 */
public class KeyRotatedException extends RuntimeException {
    public KeyRotatedException(String message) {
        super(message);
    }
}
