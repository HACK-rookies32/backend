package com.example.kidhackbackend.admin.report.controller;

import com.example.kidhackbackend.admin.report.service.AdminReportService;
import com.example.kidhackbackend.admin.report.support.CsvResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 문의 기록 CSV 리포트. 접근 권한(SUPER_ADMIN, INQUIRY_ADMIN)은
 * {@code AdminSecurityConfig#adminInquiryFilterChain} 의 {@code /api/admin/inquiries/**} 체인이 담당한다.
 */
@Tag(name = "Admin Report", description = "관리자 - 리포트(CSV) API")
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "문의 기록 CSV 다운로드",
            description = "문의 작성일 기준으로 기간을 걸러 CSV로 내려받는다. SUPER_ADMIN, INQUIRY_ADMIN 전용.")
    @GetMapping("/inquiries.csv")
    public ResponseEntity<byte[]> downloadInquiriesCsv(
            @Parameter(description = "조회 시작일(yyyy-MM-dd, 포함). 생략 시 제한 없음", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "조회 종료일(yyyy-MM-dd, 당일 포함). 생략 시 제한 없음", example = "2026-08-27")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return CsvResponse.attachment("inquiries.csv", adminReportService.exportInquiriesCsv(from, to));
    }
}
