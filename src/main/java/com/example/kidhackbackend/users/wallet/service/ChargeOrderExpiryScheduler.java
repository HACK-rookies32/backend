package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미완료(READY) 충전 주문 만료 배치 (C-2).
 *
 * <p>{@code prepare} 만 하고 {@code approve} 하지 않은 주문은 결제 의사가 없는 것으로 보고,
 * 일정 시간(기본 30분)이 지나면 EXPIRED 로 정리한다. (위치·모니터링 롤링 삭제와 같은 패턴)
 */
@Slf4j
@Component
public class ChargeOrderExpiryScheduler {

    private final ChargeOrderRepository chargeOrderRepository;
    private final int expiryMinutes;

    public ChargeOrderExpiryScheduler(ChargeOrderRepository chargeOrderRepository,
                                      @Value("${kidhack.wallet.charge-order-expiry-minutes:30}") int expiryMinutes) {
        this.chargeOrderRepository = chargeOrderRepository;
        this.expiryMinutes = expiryMinutes;
    }

    /** 매 10분마다 실행. */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void expireStaleReadyOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(expiryMinutes);
        List<ChargeOrder> stale =
                chargeOrderRepository.findByStatusAndCreatedAtBefore(ChargeStatus.READY, threshold);

        stale.forEach(ChargeOrder::markExpired);

        if (!stale.isEmpty()) {
            log.info("[CHARGE-EXPIRY] {}분 경과 READY 주문 {}건 EXPIRED 처리 (기준: {})",
                    expiryMinutes, stale.size(), threshold);
        }
    }
}
