package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardRequest;
import com.example.kidhackbackend.users.inquiry.dto.BoardResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerResponse;
import com.example.kidhackbackend.users.inquiry.entity.Board;
import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import com.example.kidhackbackend.users.inquiry.repository.BoardRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final InquiryFileStorageService inquiryFileStorageService;

    @Transactional
    public BoardResponse createBoard(Member member, BoardRequest request) {
        Board board = new Board();
        board.setMember(member);
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setAttachmentUrl(request.getAttachmentUrl());
        board.setLinkUrl(request.getLinkUrl());

        boardRepository.save(board);
        return toResponse(board);
    }

    public List<BoardResponse> getMyBoards(Member member) {
        return boardRepository.findByMemberIdOrderByCreatedAtDesc(member.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BoardDetailResponse getBoardDetail(Long boardId) {
        Board board = findBoard(boardId);
        return toDetailResponse(board);
    }

    @Transactional
    public BoardResponse updateBoard(Long boardId, Member member, BoardRequest request) {
        Board board = findBoard(boardId);

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setAttachmentUrl(request.getAttachmentUrl());
        board.setLinkUrl(request.getLinkUrl());
        //[VULN-9] board.getMember()와 파라미터로 받은 member를 비교하는 로직이 없음(IDOR)
        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(Long boardId, Member member) {
        Board board = findBoard(boardId);
        inquiryAnswerRepository.deleteByBoardId(board.getId());
        // 첨부 실물도 함께 지운다. 남겨 두면 고아 객체가 되고, 문의 첨부에는
        // 개인정보가 담기는 경우가 있어 사용자가 지웠는데 실물이 남으면 곤란하다.
        // 버저닝 덕에 즉시 소실이 아니라 복구 가능한 소프트 삭제다(infra storage.tf).
        inquiryFileStorageService.delete(board.getAttachmentUrl());
        boardRepository.delete(board);
        //[VULN-9] board.getMember()와 파라미터로 받은 member를 비교하는 로직이 없음(IDOR)
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
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
                .map(a -> new InquiryAnswerResponse(a.getId(), a.getContent(), a.getCreatedAt(), a.getUpdatedAt()))
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
}