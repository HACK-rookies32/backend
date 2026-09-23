package com.example.kidhackbackend.users.filtering.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유해 사이트/앱 접속 감지 — 역할표 No.5.
 *
 * <p>자녀가 보고한 접속 대상을 부모 블랙리스트(No.6)로 매칭해, 차단 대상이면 알림을 발행한다.
 */
@Service
@RequiredArgsConstructor
public class FilteringService {

    private final BlockRuleService blockRuleService;
    private final NotificationPort notificationPort;
    private final MemberRepository memberRepository;

    /**
     * 자녀의 접속 대상을 검사하고, 차단 대상이면 알림을 생성한다.
     *
     * @return 차단 여부
     */
    // [VULN-FILT-2] 결함이 둘이다. 2026-09-07 모의해킹에서 실제로 어디까지 뚫리는지 확인했다.
    //
    //  ① 차단 판정의 정규화가 부족하다 (실제 코드는 BlockRuleService.isBlocked).
    //     거기서 하는 정규화는 toLowerCase(Locale.ROOT) 하나뿐이다. 소문자 변환은 말 그대로
    //     "대문자를 소문자로" 만 바꾸므로, 실제 우회 성립 여부는 이렇게 갈린다.
    //       · 대소문자 변형 "YOUTUBE.COM"   → 차단된다. 소문자 변환이 정확히 이 경우만 커버한다.
    //       · 전각 문자   "ｙｏｕｔｕｂｅ.com" → 우회된다. 소문자 변환은 전각을 반각으로 바꾸지 않는다.
    //       · 호모글리프  "yоutubе.com"     → 우회된다. о·е 가 키릴 문자라 눈에만 같고 값이 다르다.
    //       · 공백/탭/제로폭 문자 삽입        → 우회된다. 글자 사이가 끊겨 contains 가 실패한다.
    //       · URL 인코딩  "you%74ube.com"   → 우회된다. 디코딩하는 단계가 아예 없다.
    //     즉 "대소문자만 막히고 나머지는 전부 통과"다. 자녀 앱이 보고 문자열만 살짝 바꾸면
    //     부모가 등록한 차단 목록이 무력화되고, 부모에게 알림도 가지 않는다.
    //     방어: 비교 전에 유니코드 정규화(Normalizer.NFKC) + 공백·제로폭 문자 제거 + URL 디코딩.
    //
    //  ② childId 를 요청 본문에 적힌 값 그대로 믿는다(본인 검증 없음 — IDOR).
    //     로그인한 자녀가 본문에 남의 childId 를 적어 보내면 그 자녀가 보고한 것으로 처리되고,
    //     엉뚱한 집 부모에게 거짓 "유해 사이트 접속 감지" 알림이 간다.
    //     방어: 인증 주체(@AuthenticationPrincipal)의 id 를 쓰고, 본문의 childId 는 무시한다.
    @Transactional
    public boolean detect(Long childId, String target) {
        boolean blocked = blockRuleService.isBlocked(childId, target);
        if (blocked) {
            // 감지 사실을 알아야 하는 쪽은 부모다. 예전에는 수신자가 자녀라
            // 부모 알림 목록에 뜨지 않았다. 미연결 자녀면 보낼 곳이 없어 건너뛴다.
            memberRepository.findById(childId)
                    .map(Member::getParent)
                    .ifPresent(parent -> notificationPort.publish(parent.getId(), childId,
                            NotificationType.HARMFUL_SITE,
                            "유해 사이트/앱 접속이 감지되었습니다: " + target));
        }
        return blocked;
    }
}
