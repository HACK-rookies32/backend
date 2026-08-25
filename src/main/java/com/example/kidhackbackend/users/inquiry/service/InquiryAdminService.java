package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerRequest;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerResponse;
import com.example.kidhackbackend.users.inquiry.entity.Board;
import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import com.example.kidhackbackend.users.inquiry.repository.BoardRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryAdminService {

    private final BoardRepository boardRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;

    public List<BoardResponse> getAllInquiries() {
        return boardRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BoardDetailResponse getInquiryDetail(Long boardId) {
        Board board = findBoard(boardId);
        return toDetailResponse(board);
    }

    @Transactional
    public InquiryAnswerResponse createAnswer(Long boardId, InquiryAnswerRequest request) {
        Board board = findBoard(boardId);
        if (inquiryAnswerRepository.findByBoardId(boardId).isPresent()) {
            throw new IllegalArgumentException("이미 답변이 등록된 문의입니다.");
        }

        InquiryAnswer answer = new InquiryAnswer();
        answer.setBoard(board);
        answer.setContent(request.getContent());
        inquiryAnswerRepository.save(answer);

        board.setAnswered(true);
        return toAnswerResponse(answer);
    }

    @Transactional
    public InquiryAnswerResponse updateAnswer(Long boardId, InquiryAnswerRequest request) {
        InquiryAnswer answer = findAnswer(boardId);
        answer.setContent(request.getContent());
        return toAnswerResponse(answer);
    }

    @Transactional
    public void deleteAnswer(Long boardId) {
        InquiryAnswer answer = findAnswer(boardId);
        inquiryAnswerRepository.delete(answer);
        findBoard(boardId).setAnswered(false);
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));
    }

    private InquiryAnswer findAnswer(Long boardId) {
        return inquiryAnswerRepository.findByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 답변이 없습니다."));
    }

    private BoardResponse toResponse(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getMember().getId(),
                board.getMember().getNickname(),
                board.getTitle(),
                board.isAnswered(),
                board.getCreatedAt()
        );
    }

    private BoardDetailResponse toDetailResponse(Board board) {
        InquiryAnswerResponse answerResponse = inquiryAnswerRepository.findByBoardId(board.getId())
                .map(this::toAnswerResponse)
                .orElse(null);

        return new BoardDetailResponse(
                board.getId(),
                board.getMember().getId(),
                board.getMember().getNickname(),
                board.getTitle(),
                board.getContent(),
                board.getAttachmentUrl(),
                board.getLinkUrl(),
                board.isAnswered(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                answerResponse
        );
    }

    private InquiryAnswerResponse toAnswerResponse(InquiryAnswer answer) {
        return new InquiryAnswerResponse(
                answer.getId(),
                answer.getContent(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}