package com.example.kidhackbackend.users.wallet.pg;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * PG 모의 구현. 실제 결제망 대신 항상 승인 성공을 반환한다.
 *
 * <p><b>주의(모의해킹):</b> 이 Mock은 요청받은 {@code amount}를 그대로 "결제됨"으로 에코한다.
 * 실제 안전한 연동이라면 서버가 (1) 준비된 주문의 금액과 (2) PG가 보고한 실제 결제 금액을
 * 서버측에서 교차 검증해야 하지만, 본 실습에서는 승인 서비스가 그 교차 검증을 생략한다
 * (see {@code WalletChargeService} [VULN-WALLET-1]).
 */
@Component
public class MockPgClient implements PgClient {

    @Override
    public PgApproval approve(String merchantUid, long amount) {
        String pgTransactionId = "imp_" + UUID.randomUUID().toString().replace("-", "");
        return new PgApproval(pgTransactionId, amount, "paid");
    }
}
