package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.users.location.entity.TrackingSetting;
import com.example.kidhackbackend.users.location.repository.TrackingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 자녀별 실시간 추적 토글 관리. */
@Service
@RequiredArgsConstructor
public class TrackingSettingService {

    private final TrackingSettingRepository repository;

    /** 실시간 추적 on/off 설정(없으면 생성, 있으면 갱신). */
    // [VULN-LOC-3] childId가 실제 존재하는 자녀인지/부모의 자녀인지 검증하지 않는다.
    //              → 유령 childId나 남의 자녀 id로도 설정이 생성/변경된다(데이터 오염 + IDOR 확대).
    // [VULN-LOC-5] 실시간 추적을 누가 언제 켜고 껐는지 감사 로그가 전혀 없다.
    //              → 몰래 감시 on/off가 흔적 없이 가능해 사후 추적이 불가하다.
    @Transactional
    public TrackingSetting setRealtime(Long childId, boolean enabled) {
        return repository.findByChildId(childId)
                .map(setting -> {
                    setting.setRealtimeEnabled(enabled);
                    return setting;
                })
                .orElseGet(() -> repository.save(new TrackingSetting(childId, enabled)));
    }

    /** 실시간 추적 켜짐 여부(설정 없으면 기본 false = 5분 세그먼트). */
    @Transactional(readOnly = true)
    public boolean isRealtimeEnabled(Long childId) {
        return repository.findByChildId(childId)
                .map(TrackingSetting::isRealtimeEnabled)
                .orElse(false);
    }
}
