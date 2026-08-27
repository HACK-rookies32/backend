package com.example.kidhackbackend.users.appcontrol.service;

import com.example.kidhackbackend.users.appcontrol.dto.SyncRequest;
import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;
import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import com.example.kidhackbackend.users.appcontrol.repository.AppControlRuleRepository;
import com.example.kidhackbackend.users.appcontrol.repository.InstalledAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppControlService {

    private final InstalledAppRepository installedAppRepository;
    private final AppControlRuleRepository ruleRepository;

    /** 자녀 설치 앱 목록 동기화. 기존 목록을 지우고 새로 저장한다. */
    @Transactional
    public int sync(SyncRequest request) {
        installedAppRepository.deleteByChildId(request.childId());

        List<InstalledApp> apps = request.apps().stream()
                .map(item -> new InstalledApp(request.childId(), item.packageName(), item.appName()))
                .toList();

        installedAppRepository.saveAll(apps);
        return apps.size();
    }

    /** 자녀 설치 앱 목록 조회. */
    public List<InstalledApp> getInstalledApps(Long childId) {
        return installedAppRepository.findByChildId(childId);
    }

    /** 통제 규칙 추가. */
    @Transactional
    public AppControlRule addRule(Long childId, String packageName, String appName, int dailyLimitMinutes) {
        return ruleRepository.save(new AppControlRule(childId, packageName, appName, dailyLimitMinutes));
    }

    /** 통제 규칙 목록 조회. */
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