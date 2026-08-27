package com.example.kidhackbackend.admin.report.controller;

import com.example.kidhackbackend.admin.report.service.AdminReportService;
import com.example.kidhackbackend.admin.report.support.CsvResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 정보 CSV 리포트. 접근 권한(SUPER_ADMIN)은
 * {@code AdminSecurityConfig#adminMemberFilterChain} 의 {@code /api/admin/members/**} 체인이 담당한다.
 */
@Tag(name = "Admin Report", description = "관리자 - 리포트(CSV) API")
@RestController
@RequestMapping("/api/admin/members/customers")
@RequiredArgsConstructor
public class AdminCustomerReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "회원 정보 CSV 다운로드", description = "고객(PARENT/CHILD) 전체를 CSV로 내려받는다. SUPER_ADMIN 전용.")
    @GetMapping("/customers.csv")
    public ResponseEntity<byte[]> downloadCustomersCsv() {
        return CsvResponse.attachment("customers.csv", adminReportService.exportCustomersCsv());
    }
}
