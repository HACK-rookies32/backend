package com.example.kidhackbackend.admin.audit.service;

import com.example.kidhackbackend.admin.audit.dto.AuditLogResponse;
import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.entity.AdminAuditLog;
import com.example.kidhackbackend.admin.audit.repository.AdminAuditLogRepository;
import com.example.kidhackbackend.admin.audit.repository.AdminAuditLogSpecification;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 활동 이력 기록·조회.
 *
 * <p><b>기록</b>은 admin 패키지의 변경(비-GET) 엔드포인트가 성공한 뒤 컨트롤러에서 부른다.
 * 서비스가 아니라 컨트롤러에서 부르는 이유는 두 가지다 — (1) 공지사항처럼 사용자 화면과 서비스를
 * 공유하는 기능이 있어 서비스에서 기록하면 관리자가 아닌 호출까지 섞인다, (2) 기록 지점이 한 계층에
 * 모여 있어야 "관리자 변경 API 를 추가했는데 감사로그를 빠뜨렸다" 를 리뷰에서 잡아낼 수 있다.
 * 예외는 자금 변경(강제환불·잔액조정) 둘뿐이다 — 대상 id 가 서비스 안에서야 확정되고 돈이 움직인
 * 트랜잭션과 같이 커밋돼야 해서 {@code AdminFinanceCommandService} 안에서 부른다.
 *
 * <p>기록은 기본 전파(REQUIRED)다. 컨트롤러에서 부르면 작업이 끝난 뒤 별도 트랜잭션으로 커밋되고,
 * 자금 변경처럼 트랜잭션 안에서 부르면 그 트랜잭션에 합류해 함께 커밋된다.
 *
 * <p>인증 주체를 못 찾으면 예외를 던지지 않고 경고 로그만 남긴다 — 이미 성공한 관리자 작업을
 * 감사로그 때문에 실패로 되돌리지는 않되, 이력이 빠진 사실은 운영 로그에 드러나야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    /** 명세서상 페이지당 건수는 20 고정이다. 클라이언트가 바꿀 수 없다. */
    private static final int PAGE_SIZE = 20;

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final MemberRepository memberRepository;

    // ------------------------------------------------------------------
    // 기록
    // ------------------------------------------------------------------

    /**
     * 현재 인증된 관리자의 활동을 기록한다. 대상 리소스 종류는 활동 유형이 결정한다.
     *
     * @param targetId 대상 리소스 ID. 전체 알림 발송처럼 대상이 없으면 null.
     */
    @Transactional
    public void record(AdminActivityType activityType, Long targetId) {
        record(activityType, targetId, null);
    }

    /**
     * 사유를 함께 남기는 기록. 관리자가 입력한 문구가 {@code description} 에 그대로 들어간다.
     *
     * <p>충전 취소처럼 "왜 했는지" 가 있어야 이력이 의미를 갖는 활동에 쓴다. 사유를 받지 않는 활동은
     * 인자 2개짜리를 그대로 쓰면 된다.
     */
    @Transactional
    public void record(AdminActivityType activityType, Long targetId, String description) {
        Member admin = currentAdmin();
        if (admin == null) {
            // 관리자 시큐리티 체인 뒤에서만 불리므로 정상 흐름에서는 나올 수 없다.
            log.warn("감사로그를 남기지 못했다 — 인증 주체를 찾을 수 없다. activityType={}, targetId={}",
                    activityType, targetId);
            return;
        }
        record(admin, activityType, targetId, description);
    }

    /**
     * 관리자 로그인 기록.
     *
     * <p>로그인은 토큰을 <b>발급하는</b> 요청이라 아직 SecurityContext 가 비어 있다. 그래서 이 경로만
     * 로그인 ID 로 계정을 되찾아 기록한다. 대상은 로그인한 관리자 자신이다.
     */
    @Transactional
    public void recordAdminLogin(String loginId) {
        memberRepository.findByLoginId(loginId).ifPresentOrElse(
                admin -> record(admin, AdminActivityType.ADMIN_LOGIN, admin.getId()),
                () -> log.warn("감사로그를 남기지 못했다 — 계정을 찾을 수 없다. loginId={}", loginId));
    }

    @Transactional
    public void record(Member admin, AdminActivityType activityType, Long targetId) {
        record(admin, activityType, targetId, null);
    }

    @Transactional
    public void record(Member admin, AdminActivityType activityType, Long targetId, String description) {
        adminAuditLogRepository.save(
                AdminAuditLog.of(admin.getId(), admin.getLoginId(), activityType, targetId, description));
    }

    /**
     * JWT 필터가 심어 둔 인증 주체에서 관리자를 꺼낸다.
     *
     * <p>필터는 회원을 찾으면 {@link Member}, 못 찾으면 로그인 ID 문자열을 주체로 넣는다. 두 경우를
     * 모두 처리해야 토큰은 유효한데 주체가 문자열인 상황에서도 이력이 비지 않는다.
     */
    private Member currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Member member) {
            return member;
        }
        if (principal instanceof String loginId) {
            return memberRepository.findByLoginId(loginId).orElse(null);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 조회
    // ------------------------------------------------------------------

    /**
     * 조건별 활동 이력 페이징 조회. 발생일시 내림차순.
     *
     * @param from 조회 시작일. 해당일 00:00:00 부터.
     * @param to   조회 종료일. 해당일 23:59:59.999999999 까지.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String adminLoginId, String activityTypeCode,
                                         LocalDate from, LocalDate to, int page) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이후일 수 없습니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        AdminActivityType activityType = AdminActivityType.from(activityTypeCode);
        LocalDateTime fromAt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toAt = (to != null) ? to.atTime(LocalTime.MAX) : null;

        // 같은 초에 여러 건이 쌓이면 발생일시만으로는 순서가 흔들린다. id 를 2차 정렬로 붙여
        // 페이지를 넘길 때 같은 건이 두 번 보이거나 누락되는 일을 막는다.
        Pageable pageable = PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "occurredAt", "id"));

        return adminAuditLogRepository
                .findAll(AdminAuditLogSpecification.search(adminLoginId, activityType, fromAt, toAt), pageable)
                .map(AuditLogResponse::from);
    }
}
