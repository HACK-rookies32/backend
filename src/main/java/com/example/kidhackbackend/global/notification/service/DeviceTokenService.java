package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    /**
     * FCM 토큰 등록/갱신.
     *
     * <p>기기(토큰)를 키로 본다. 같은 토큰 행이 이미 있으면 주인을 이 회원으로 바꾸고
     * (기기 공유 후 계정만 바꿔 로그인한 경우 이전 사용자에게 가던 푸시를 끊는다),
     * 없으면 새로 만든다. 한 회원이 기기 여러 대를 등록할 수 있다.
     */
    @Transactional
    public void register(Long memberId, String token) {
        repository.findByToken(token)
                .ifPresentOrElse(
                        existing -> existing.assignTo(memberId),
                        () -> repository.save(new DeviceToken(memberId, token))
                );
    }

    /** 로그아웃 시 그 기기 토큰만 해제. 없으면 조용히 통과(멱등). */
    @Transactional
    public void unregister(Long memberId, String token) {
        repository.deleteByTokenAndMemberId(token, memberId);
    }

    /** 내가 등록한 기기 토큰들. */
    public List<DeviceToken> getMyTokens(Long memberId) {
        return repository.findByMemberId(memberId);
    }
}
