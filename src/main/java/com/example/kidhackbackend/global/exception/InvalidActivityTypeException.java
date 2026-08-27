package com.example.kidhackbackend.global.exception;

/**
 * 감사로그 조회에서 정의되지 않은 활동 유형 코드를 받았을 때. 400 {@code INVALID_ACTIVITY_TYPE}.
 *
 * <p>{@code IllegalArgumentException}(→ {@code INVALID_PARAMETER}) 과 코드가 달라야 해서
 * 별도 예외로 둔다. 프론트가 "날짜가 잘못됐다" 와 "활동 유형이 잘못됐다" 를 구분해 안내한다.
 */
public class InvalidActivityTypeException extends RuntimeException {
    public InvalidActivityTypeException(String message) {
        super(message);
    }
}
