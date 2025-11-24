//package com.project.gugumarket.dto;
//
//import com.project.gugumarket.entity.Report;
//import lombok.*;
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ReportResponseDto {
//
//    private Long reportId;
//    private Long productId;
//    private String productTitle;
//    private Long reporterId;
//    private String reporterName;
//    private String reason;
//    private LocalDateTime createdDate;
//
//    public static ReportResponseDto fromEntity(Report report) {
//        return ReportResponseDto.builder()
//                .reportId(report.getReportId())
//                .productId(report.getProduct() != null ? report.getProduct().getProductId() : null)
//                .productTitle(report.getProduct() != null ? report.getProduct().getTitle() : null)
//                .reporterId(report.getReporter() != null ? report.getReporter().getUserId() : null)
//                .reporterName(report.getReporter() != null ? report.getReporter().getNickname() : null)
//                .reason(report.getReason())
//                .createdDate(report.getCreatedDate())
//                .build();
//    }
//}

package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Report;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDto {

    private Long reportId;
    private Long productId;
    private String productTitle;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private LocalDateTime createdDate;
    private String status; // 👈 추가!

    public static ReportResponseDto fromEntity(Report report) {
        return ReportResponseDto.builder()
                .reportId(report.getReportId())
                .productId(report.getProduct() != null ? report.getProduct().getProductId() : null)
                .productTitle(report.getProduct() != null ? report.getProduct().getTitle() : null)
                .reporterId(report.getReporter() != null ? report.getReporter().getUserId() : null)
                .reporterName(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reason(report.getReason())
                .createdDate(report.getCreatedDate())
                .status(report.getStatus() != null ? report.getStatus().name() : "PENDING") // 👈 추가!
                .build();
    }
}