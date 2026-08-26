package com.example.kidhackbackend.admin.member.controller;

import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerResponse;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerUpdateRequest;
import com.example.kidhackbackend.admin.member.dto.AdminDeviceResponse;
import com.example.kidhackbackend.admin.member.service.AdminCustomerService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Customer", description = "관리자 - 고객(회원) 관리 API")
@RestController
@RequestMapping("/api/admin/members/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCustomerResponse>>> getCustomers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminCustomerService.getCustomers(role, keyword, PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerResponse>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getCustomer(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerResponse>> updateCustomer(
            @PathVariable Long id, @RequestBody AdminCustomerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.updateCustomer(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        adminCustomerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/devices")
    public ResponseEntity<ApiResponse<List<AdminDeviceResponse>>> getDevices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getDevices(id)));
    }

    @GetMapping("/{id}/subscription")
    public ResponseEntity<ApiResponse<AdminUserWalletResponse>> getSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getSubscription(id)));
    }
}