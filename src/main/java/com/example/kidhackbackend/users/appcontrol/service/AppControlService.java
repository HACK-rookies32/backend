package com.example.kidhackbackend.users.appcontrol.service;

import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.users.appcontrol.dto.SyncRequest;
import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;
import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import com.example.kidhackbackend.users.appcontrol.repository.AppControlRuleRepository;
import com.example.kidhackbackend.users.appcontrol.repository.InstalledAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppControlService {

    private final InstalledAppRepository installedAppRepository;
    private final AppControlRuleRepository ruleRepository;

    /**
     * 자녀 설치 앱 목록 동기화. 기존 목록을 전부 지우고 새로 저장한다(전량 교체).
     *
     * <p>[VULN-APPCTRL-1] childId 를 요청 값으로만 지정하고 요청자(자녀 본인)인지 검증하지 않는다(IDOR).
     *   게다가 저장 전에 {@code deleteByChildId} 로 대상 자녀의 기존 목록을 통째로 삭제하므로,
     *   임의 childId 로 호출하면 다른 자녀의 설치앱 목록을 파괴하거나 위조할 수 있다.
     */
    @Transactional
    public int sync(SyncRequest request) {
        installedAppRepository.deleteByChildId(request.childId());

        // [G-2] 요청 리스트에 같은 packageName 이 중복이면 (childId, packageName) unique 제약 위반 → 500 이 났다.
        //       마지막 값이 이기도록 dedup 한 뒤 저장한다.
        Map<String, InstalledApp> byPackage = new LinkedHashMap<>();
        for (SyncRequest.AppItem item : request.apps()) {
            byPackage.put(item.packageName(),
                    new InstalledApp(request.childId(), item.packageName(), item.appName()));
        }

        installedAppRepository.saveAll(byPackage.values());
        return byPackage.size();
    }

    /** 자녀 설치 앱 목록 조회. [VULN-APPCTRL-3] childId 소유권 미검증(IDOR) — 임의 자녀 목록 열람 가능. */
    public List<InstalledApp> getInstalledApps(Long childId) {
        return installedAppRepository.findByChildId(childId);
    }

    /**
     * 통제 규칙 추가.
     *
     * <p>[VULN-APPCTRL-2] childId 소유권 미검증(IDOR) — 임의 자녀에게 통제 규칙을 생성할 수 있다.
     */
    @Transactional
    public AppControlRule addRule(Long childId, String packageName, String appName, int dailyLimitMinutes) {
        // [G-1] (childId, packageName) 은 unique 라 중복 등록 시 제약 위반 → 500 이 났다. 409 로 명확히 끊는다.
        if (ruleRepository.existsByChildIdAndPackageName(childId, packageName)) {
            throw new DuplicateResourceException("이미 통제 규칙이 등록된 앱입니다.");
        }
        return ruleRepository.save(new AppControlRule(childId, packageName, appName, dailyLimitMinutes));
    }

    /** 통제 규칙 목록 조회. [VULN-APPCTRL-3] childId 소유권 미검증(IDOR) — 임의 자녀 규칙 열람 가능. */
    public List<AppControlRule> getRules(Long childId) {
        return ruleRepository.findByChildId(childId);
    }

    /** 통제 규칙 수정 (시간/활성화). */
    @Transactional
    public AppControlRule updateRule(Long ruleId, Integer dailyLimitMinutes, Boolean enabled) {
        AppControlRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 통제 규칙입니다."));
        rule.update(dailyLimitMinutes, enabled);
        return rule;
    }

    /** 통제 규칙 삭제. */
    @Transactional
    public void deleteRule(Long ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("존재하지 않는 통제 규칙입니다.");
        }
        ruleRepository.deleteById(ruleId);
    }
}
