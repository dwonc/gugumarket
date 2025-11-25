package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    //  추가
    long countByProduct_ProductId(Long productId);

    // 🎯🔥 중복 신고 체크 메서드 추가 🔥🎯
    boolean existsByProduct_ProductIdAndReporter_UserId(Long productId, Long userId);

    // 🎯 User별 신고 목록 조회 추가
    List<Report> findByReporter_UserIdOrderByCreatedDateDesc(Long userId);

}