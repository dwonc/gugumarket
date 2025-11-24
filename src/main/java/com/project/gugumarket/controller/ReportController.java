package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ReportResponseDto;
import com.project.gugumarket.entity.Report;
import com.project.gugumarket.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/report")  // 👈 변경!
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestBody Map<String, Object> request,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            Long productId = Long.parseLong(request.get("productId").toString());
            String reason = request.getOrDefault("reason", "부적절한 게시물").toString();

            reportService.createReport(productId, principal.getName(), reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "신고가 접수되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "신고 접수 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/admin/list")
    public ResponseEntity<?> getReports() {
        try {
            List<Report> reports = reportService.getAllReports();

            List<ReportResponseDto> reportDtos = reports.stream()
                    .map(ReportResponseDto::fromEntity)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reports", reportDtos
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "조회 실패"));
        }
    }

    // 추가 =================================================================
    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long reportId) {
        try {
            reportService.updateReportStatus(reportId, Report.ReportStatus.RESOLVED);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "처리 완료되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "처리 실패"));
        }
    }


}