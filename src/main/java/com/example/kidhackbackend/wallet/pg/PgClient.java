package com.example.kidhackbackend.wallet.pg;

/**
 * PG 연동 인터페이스(공통). 실제 PG(포트원 등) 대신 {@link MockPgClient}로 대체한다.
 *
 * <p>웹훅 서명검증(No.36)에서 이 인터페이스 구현에 서명 검증 로직이 추가된다.
 */
public interface PgClient {

    /**
     * 준비된 주문번호로 결제 승인을 확인한다.
     *
     * @param merchantUid 가맹점 주문번호(준비 단계 발급)
     * @param amount      승인 요청 금액(원)
     * @return 승인 결과
     */
    PgApproval approve(String merchantUid, long amount);
}
