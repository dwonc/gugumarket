//package com.project.gugumarket.service;
//
//import com.project.gugumarket.entity.Product;
//import com.project.gugumarket.entity.Report;
//import com.project.gugumarket.entity.User;
//import com.project.gugumarket.repository.ProductRepository;
//import com.project.gugumarket.repository.ReportRepository;
//import com.project.gugumarket.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class ReportService {
//
//    private final ReportRepository reportRepository;
//    private final ProductRepository productRepository;
//    private final UserRepository userRepository;
//
//    @Transactional
//    public Report createReport(Long productId, String username, String reason) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
//
//        User reporter = userRepository.findByUserName(username)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//        if (reportRepository.existsByProduct_ProductIdAndReporter_UserId(productId, reporter.getUserId())) {
//            throw new RuntimeException("동일 게시물의 중복신고는 안됩니다");
//        }
//
//        Report report = Report.builder()
//                .product(product)
//                .reporter(reporter)
//                .reason(reason)
//                .build();
//
//        return reportRepository.save(report);
//    }
//
//    @Transactional(readOnly = true)
//    public List<Report> getAllReports() {
//        return reportRepository.findAll();
//    }
//
//    // 🎯🎯🎯 User별 신고 목록 조회 추가 🎯🎯🎯🎯
//    @Transactional(readOnly = true)
//    public List<Report> getMyReports(String username) {
//        User user = userRepository.findByUserName(username)
//                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//        return reportRepository.findByReporter_UserIdOrderByCreatedDateDesc(user.getUserId());
//    }
//
//    @Transactional
//    public void updateReportStatus(Long reportId, Report.ReportStatus status) {
//        Report report = reportRepository.findById(reportId)
//                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));
//
//        report.setStatus(status);
//        reportRepository.save(report);
//    }
//
//    @Transactional(readOnly = true)
//    public long getReportCountByProduct(Long productId) {
//        return reportRepository.countByProduct_ProductId(productId);
//    }
//}

//---------------------------------------------------------------------------------------

package com.project.gugumarket.service;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Report;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.ReportRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
// 🎯🔥✨ [추가 2 시작] Slf4j import 추가 ✨🔥🎯
import lombok.extern.slf4j.Slf4j;
// 🎯🔥✨ [추가 2 끝] ✨🔥🎯
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
// 🎯🔥✨ [추가 3 시작] Slf4j 어노테이션 추가 ✨🔥🎯
@Slf4j
// 🎯🔥✨ [추가 3 끝] ✨🔥🎯
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    // 🎯🔥✨💫⭐ [추가 4 시작] NotificationService 주입 ⭐💫✨🔥🎯
    private final NotificationService notificationService;
    // 🎯🔥✨💫⭐ [추가 4 끝] ⭐💫✨🔥🎯

    @Transactional
    public Report createReport(Long productId, String username, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        User reporter = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (reportRepository.existsByProduct_ProductIdAndReporter_UserId(productId, reporter.getUserId())) {
            throw new RuntimeException("동일 게시물의 중복신고는 안됩니다");
        }

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

    @Transactional(readOnly = true)
    public List<Report> getMyReports(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return reportRepository.findByReporter_UserIdOrderByCreatedDateDesc(user.getUserId());
    }

    // 🎯🔥✨💫⭐🌟 [수정 2 시작] 알림 생성 로직 추가 🌟⭐💫✨🔥🎯
    @Transactional
    public void updateReportStatus(Long reportId, Report.ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 내역을 찾을 수 없습니다."));

        report.setStatus(status);
        reportRepository.save(report);

        // 🎯 처리 완료 시 신고자에게 알림 전송
        if (status == Report.ReportStatus.RESOLVED) {
            try {
                notificationService.createReportResolvedNotification(report);
                log.info("✅ 신고 처리 알림 전송 완료 - 신고 ID: {}, 신고자: {}",
                        reportId, report.getReporter().getNickname());
            } catch (Exception e) {
                log.error("❌ 신고 처리 알림 전송 실패 - 신고 ID: {}", reportId, e);
                // 알림 실패해도 신고 처리는 완료되도록 예외를 먹음
            }
        }
    }
    // 🎯🔥✨💫⭐🌟 [수정 2 끝] 🌟⭐💫✨🔥🎯

    @Transactional(readOnly = true)
    public long getReportCountByProduct(Long productId) {
        return reportRepository.countByProduct_ProductId(productId);
    }
}