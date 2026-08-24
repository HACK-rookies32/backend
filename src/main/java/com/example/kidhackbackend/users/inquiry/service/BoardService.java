package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardRequest;
import com.example.kidhackbackend.users.inquiry.dto.BoardResponse;
import com.example.kidhackbackend.users.inquiry.entity.Board;
import com.example.kidhackbackend.users.inquiry.repository.BoardRepository;
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

    public List<BoardResponse> getAllBoards() {
        return boardRepository.findAllByOrderByCreatedAtDesc().stream()
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
        return toResponse(board);
    }

    @Transactional
    public void deleteBoard(Long boardId, Member member) {
        Board board = findBoard(boardId);
        boardRepository.delete(board);
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
                board.getCreatedAt()
        );
    }

    private BoardDetailResponse toDetailResponse(Board board) {
        return new BoardDetailResponse(
                board.getId(),
                board.getMember().getId(),
                board.getMember().getNickname(),
                board.getTitle(),
                board.getContent(),
                board.getAttachmentUrl(),
                board.getLinkUrl(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}