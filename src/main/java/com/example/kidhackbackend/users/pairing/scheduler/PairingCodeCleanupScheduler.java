package com.example.kidhackbackend.users.pairing.scheduler;

import com.example.kidhackbackend.users.pairing.repository.PairingCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 페어링 코드 정리.
 *
 * <p>사용된 코드는 {@code PairingService.connect} 에서 즉시 삭제되고, 사용되지 않은 코드는
 * 만료(발급 + {@code CODE_TTL_MINUTES}) 후 이 스케줄러가 1분마다 지운다.
 */
@Component
@RequiredArgsConstructor
public class PairingCodeCleanupScheduler {

    private final PairingCodeRepository pairingCodeRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredCodes() {
        pairingCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
