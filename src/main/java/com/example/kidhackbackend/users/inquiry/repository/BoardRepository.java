package com.example.kidhackbackend.users.inquiry.repository;

import com.example.kidhackbackend.users.inquiry.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderByCreatedAtDesc();
}