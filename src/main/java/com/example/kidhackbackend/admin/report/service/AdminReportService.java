package com.example.kidhackbackend.admin.report.service;

import com.example.kidhackbackend.admin.member.dto.AdminCustomerResponse;
import com.example.kidhackbackend.admin.member.service.AdminCustomerService;
import com.example.kidhackbackend.admin.report.support.CsvBuilder;
import com.example.kidhackbackend.users.inquiry.entity.Board;
import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import com.example.kidhackbackend.users.inquiry.repository.BoardRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 관리자 리포트(CSV 다운로드) 생성. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    // 기간 미지정 시 사용할 경계값. 파라미터 null 바인딩을 피하려고 실제 데이터가 존재할 수 없는 범위로 잡는다.
    private static final LocalDateTime PERIOD_MIN = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime PERIOD_MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final AdminCustomerService adminCustomerService;
    private final BoardRepository boardRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;

    /**
     * 회원(고객) 정보 CSV. 고객 조회 화면(GET /api/admin/members/customers)과 같은 조회를 그대로 쓰므로
     * 대상 기준(PARENT/CHILD)이 화면과 항상 일치한다. 탈퇴 회원은 '탈퇴여부' 컬럼으로 구분해 함께 내보낸다.
     */
    public byte[] exportCustomersCsv() {
        CsvBuilder csv = new CsvBuilder(
                "회원ID", "로그인ID", "이름", "닉네임", "이메일", "전화번호",
                "주소", "권한", "부모ID", "탈퇴여부", "가입일", "수정일");

        adminCustomerService.getAllCustomers().stream()
                .sorted(Comparator.comparing(AdminCustomerResponse::id))
                .forEach(customer -> csv.row(
                        customer.id(),
                        customer.loginId(),
                        customer.username(),
                        customer.nickname(),
                        customer.email(),
                        customer.phoneNumber(),
                        customer.address(),
                        customer.role(),
                        customer.parentId(),
                        customer.isDeleted() ? "Y" : "N",
                        CsvBuilder.format(customer.createdAt()),
                        CsvBuilder.format(customer.updatedAt())));

        return csv.toBytes();
    }

    /**
     * 문의 기록 CSV. 문의 작성일 기준으로 기간을 거르며, from/to 는 둘 다 선택이고 to 는 해당 일자까지 포함한다.
     */
    public byte[] exportInquiriesCsv(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 이후일 수 없습니다.");
        }
        LocalDateTime start = (from != null) ? from.atStartOfDay() : PERIOD_MIN;
        LocalDateTime end = (to != null) ? to.atTime(LocalTime.MAX) : PERIOD_MAX;

        List<Board> boards = boardRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        Map<Long, InquiryAnswer> answers = findAnswers(boards);

        CsvBuilder csv = new CsvBuilder(
                "문의ID", "작성자ID", "작성자닉네임", "제목", "내용", "첨부파일URL", "링크URL",
                "답변여부", "작성일", "수정일", "답변내용", "답변일");

        for (Board board : boards) {
            InquiryAnswer answer = answers.get(board.getId());
            csv.row(
                    board.getId(),
                    board.getMember().getId(),
                    board.getMember().getNickname(),
                    board.getTitle(),
                    board.getContent(),
                    board.getAttachmentUrl(),
                    board.getLinkUrl(),
                    board.isAnswered() ? "Y" : "N",
                    CsvBuilder.format(board.getCreatedAt()),
                    CsvBuilder.format(board.getUpdatedAt()),
                    answer != null ? answer.getContent() : null,
                    answer != null ? CsvBuilder.format(answer.getCreatedAt()) : null);
        }

        return csv.toBytes();
    }

    private Map<Long, InquiryAnswer> findAnswers(List<Board> boards) {
        if (boards.isEmpty()) {
            return Map.of();
        }
        List<Long> boardIds = boards.stream().map(Board::getId).toList();
        return inquiryAnswerRepository.findByBoardIdIn(boardIds).stream()
                .collect(Collectors.toMap(answer -> answer.getBoard().getId(), Function.identity()));
    }
}
