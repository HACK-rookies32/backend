package com.example.kidhackbackend.users.wallet.pg;

/**
 * PG 연동 인터페이스(공통). 실제 PG(포트원 등) 대신 {@link MockPgClient}로 대체한다.
 *
 * <p>웹훅 서명검증(No.36)에서 이 인터페이스 구현에 서명 검증 로직이 추가된다.
 */
public interface PgClient {

    /**
     * 준비된 주문에 대한 결제창 정보를 만든다. (prepare 응답에 실려 프론트로 전달)
     *
     * @param merchantUid 가맹점 주문번호(준비 단계 발급)
     * @param amount      결제 예정 금액(원)
     * @return 결제창 정보(PG사·checkoutUrl·mock 여부)
     */
    PgCheckout checkout(String merchantUid, long amount);

    /**
     * 준비된 주문번호로 결제 승인을 확인한다.
     *
     * @param merchantUid 가맹점 주문번호(준비 단계 발급)
     * @param amount      승인 요청 금액(원)
     * @return 승인 결과
     */
    PgApproval approve(String merchantUid, long amount);

    /**
     * 웹훅 본문에 대한 서명(HMAC-SHA256 hex)을 계산한다. (No.36 — 테스트/모의 PG가 서명 생성용)
     *
     * @param rawBody 웹훅 원본 바디(직렬화된 JSON 문자열)
     * @return hex 인코딩된 서명
     */
    String sign(String rawBody);

    /**
     * 웹훅 서명을 검증한다.
     *
     * @param rawBody   웹훅 원본 바디
     * @param signature 요청 헤더로 전달된 서명
     * @return 서명이 유효하면 true (signature가 null이면 false)
     */
    boolean verify(String rawBody, String signature);
}
