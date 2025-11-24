package com.project.gugumarket.service;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Report;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.ReportRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Report createReport(Long productId, String username, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        User reporter = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 🎯🔥 중복 신고 방지 - 여기 추가! 🔥🎯
//        if (reportRepository.existsByProduct_ProductIdAndReporter_UserId(productId, reporter.getUserId())) {
//            throw new RuntimeException("이미 신고한 상품입니다.");
//        }

        Report report = Report.builder()
                .product(product)
                .reporter(reporter)
                .reason(reason)
                .build();

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }


    // 추가=========================================================(신고처리상태)
    @Transactional
    public void updateReportStatus(Long reportId, Report.ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));

        report.setStatus(status);
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public long getReportCountByProduct(Long productId) {
        System.out.println("🔍 ReportService - productId: " + productId);
        long count = reportRepository.countByProduct_ProductId(productId);
        System.out.println("🔍 ReportService - count: " + count);
        return count;
    }

}