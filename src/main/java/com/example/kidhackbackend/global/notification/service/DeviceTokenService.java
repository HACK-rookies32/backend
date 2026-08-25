package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    @Transactional
    public void register(Long memberId, String token) {
        repository.findByMemberId(memberId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(token),
                        () -> repository.save(new DeviceToken(memberId, token))
                );
    }


    public DeviceToken getMyToken(Long memberId) {
        return repository.findByMemberId(memberId)
                .orElse(null); // 임시 테스트용: 없으면 null 반환
    }
}