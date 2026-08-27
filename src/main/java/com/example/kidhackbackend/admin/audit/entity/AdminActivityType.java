package com.example.kidhackbackend.admin.audit.entity;

import com.example.kidhackbackend.global.exception.InvalidActivityTypeException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사로그로 남기는 관리자 활동 유형.
 *
 * <p>대상은 admin 패키지의 <b>조회(GET)를 제외한 모든 엔드포인트</b>다. 유형마다 대상 리소스 종류가
 * 하나로 정해지므로 {@link AuditTargetType} 을 값에 함께 묶어 둔다. 기록하는 쪽은 유형과 대상 ID 만
 * 넘기면 되고, "회원 삭제인데 targetType 이 POST" 같은 조합 실수가 원천적으로 생기지 않는다.
 *
 * <p>{@code label} 은 응답의 {@code activityLabel} 로 그대로 나간다 — 표시 문구를 서버가 확정한다.
 */
@Getter
@RequiredArgsConstructor
public enum AdminActivityType {

    // 인증 — POST /api/admin/auth/login
    ADMIN_LOGIN("관리자 로그인", AuditTargetType.MEMBER),

    // 회원 — PATCH/DELETE /api/admin/members/customers/{id}, PATCH /api/admin/members/{id}/role
    MEMBER_UPDATE("회원 정보 수정", AuditTargetType.MEMBER),
    MEMBER_DELETE("회원 삭제", AuditTargetType.MEMBER),
    MEMBER_ROLE_UPDATE("관리자 권한 변경", AuditTargetType.MEMBER),

    // 커뮤니티 — POST /api/admin/community/members/ban, DELETE /api/admin/community/posts/{postId}
    MEMBER_BAN("계정 정지", AuditTargetType.MEMBER),
    POST_DELETE("게시글 삭제", AuditTargetType.POST),

    // 자금 — POST /api/admin/finance/users/{userId}/adjust, .../payments/{qrToken}/force-refund
    WALLET_BALANCE_ADJUST("지갑 잔액 수동 조정", AuditTargetType.WALLET),
    PAYMENT_REFUND("환불 처리", AuditTargetType.PAYMENT),

    // 1:1 문의 — POST/PUT/DELETE /api/admin/inquiries/{id}/answer
    INQUIRY_ANSWER_CREATE("문의 답변 등록", AuditTargetType.INQUIRY),
    INQUIRY_ANSWER_UPDATE("문의 답변 수정", AuditTargetType.INQUIRY),
    INQUIRY_ANSWER_DELETE("문의 답변 삭제", AuditTargetType.INQUIRY),

    // 공지사항 — POST/PATCH/DELETE /api/admin/notices
    NOTICE_CREATE("공지사항 등록", AuditTargetType.NOTICE),
    NOTICE_UPDATE("공지사항 수정", AuditTargetType.NOTICE),
    NOTICE_DELETE("공지사항 삭제", AuditTargetType.NOTICE),

    // 알림 — POST /api/admin/notifications/broadcast
    NOTIFICATION_BROADCAST("전체 알림 발송", AuditTargetType.NOTIFICATION),

    // 유해 키워드 — POST/DELETE /api/admin/harmful-keywords
    HARMFUL_KEYWORD_ADD("차단 키워드 추가", AuditTargetType.HARMFUL_KEYWORD),
    HARMFUL_KEYWORD_DELETE("차단 키워드 삭제", AuditTargetType.HARMFUL_KEYWORD);

    private final String label;
    private final AuditTargetType targetType;

    /**
     * 조회 파라미터로 들어온 활동 유형 코드를 열거형으로 바꾼다.
     *
     * <p>{@code @RequestParam AdminActivityType} 로 바인딩하면 정의되지 않은 코드가
     * {@code INVALID_PARAMETER} 로 떨어진다. 명세서는 이 경우 {@code INVALID_ACTIVITY_TYPE} 이므로
     * 문자열로 받아 여기서 직접 변환한다.
     */
    public static AdminActivityType from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return AdminActivityType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidActivityTypeException("정의되지 않은 활동 유형입니다: " + code);
        }
    }
}
