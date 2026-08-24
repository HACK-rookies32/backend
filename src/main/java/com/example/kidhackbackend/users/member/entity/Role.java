package com.example.kidhackbackend.users.member.entity;

public enum Role {
    SUPER_ADMIN,      // 슈퍼관리자
    GENERAL_ADMIN,    // 일반관리자 (관리자 중 최하위)
    COMMUNITY_ADMIN,  // 커뮤니티 게시판 관리자
    INQUIRY_ADMIN,    // 1:1 문의 게시판 관리자
    PARENT,           // 부모
    CHILD             // 자녀
}