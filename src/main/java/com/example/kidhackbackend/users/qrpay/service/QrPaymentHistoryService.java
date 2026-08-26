package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrPaymentHistoryPageResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrPaymentHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자녀 결제 내역 조회 — QR결제 No.45. 결제된(PAID/CANCELLED) QR을 페이징으로 조회한다.
 *
 * <p>CHILD는 본인 내역, PARENT는 지정한 childId의 내역을 본다. 매장명·금액·상태·결제시각을 포함.
 */
@Service
@RequiredArgsConstructor
public class QrPaymentHistoryService {

    private final QrPaymentRepository qrPaymentRepository;

    /**
     * 결제 내역 페이징 조회.
     *
     * @param requester 로그인한 회원(PARENT/CHILD)
     * @param childId   PARENT가 조회할 자녀 id(CHILD는 무시)
     */
    @Transactional(readOnly = true)
    public QrPaymentHistoryPageResponse getPayments(Member requester, Long childId, int page, int size) {
        Long targetChildId;
        if (requester.getRole() == Role.CHILD) {
            // 자녀 본인 내역: JWT의 자기 id를 사용(childId 파라미터 무시) → 남의 것 조회 불가(안전).
            targetChildId = requester.getId();
        } else {
            // PARENT 경로.
            if (childId == null) {
                throw new IllegalArgumentException("childId는 필수입니다.");
            }
            // [VULN-WALLET-19] 페어링 미검증(IDOR / BOLA).
            //   childId가 실제 이 부모(requester)의 자녀인지 확인하지 않는다('본인 자녀만' 위반).
            //   → 아무 PARENT나 임의 childId로 남의 자녀 결제 내역(매장·금액·시각)을 열람할 수 있다.
            //   방어: memberRepository.findById(childId) 후 child.getParent().getId()==requester.getId() + CHILD 확인.
            targetChildId = childId;
        }

        Pageable pageable = PageRequest.of(page, size);
        return QrPaymentHistoryPageResponse.from(
                qrPaymentRepository.findByChildIdAndPaidAtIsNotNullOrderByIdDesc(targetChildId, pageable)
                        .map(QrPaymentHistoryResponse::from));
    }
}
