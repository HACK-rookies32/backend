package com.example.kidhackbackend.global.notification.push;

/**
 * 푸시 한 건의 발송 결과.
 *
 * <p>호출한 쪽이 "이 토큰을 지워야 하는가" 를 판단할 수 있어야 해서 성공/실패만으로는
 * 부족하다. 실패를 {@link #TOKEN_INVALID} 와 {@link #FAILED} 로 나누는 이유다.
 */
public enum PushResult {

    SUCCESS,

    /**
     * 토큰이 더 이상 유효하지 않다(앱 삭제·재설치 등). 저장된 토큰을 지워도 된다.
     */
    TOKEN_INVALID,

    /**
     * 그 밖의 실패(네트워크·인증·서버 오류). <b>토큰을 지우면 안 된다</b> —
     * 일시적인 문제일 뿐인데 멀쩡한 토큰을 날리게 된다.
     */
    FAILED
}
