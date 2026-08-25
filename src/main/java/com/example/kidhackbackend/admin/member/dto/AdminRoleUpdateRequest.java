package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AdminRoleUpdateRequest {

    @NotNull
    private Role role;
}