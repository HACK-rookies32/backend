package com.example.kidhackbackend.admin.member.dto;

public record AdminCustomerUpdateRequest(
        String nickname, String email, String phoneNumber, String address
) {
}