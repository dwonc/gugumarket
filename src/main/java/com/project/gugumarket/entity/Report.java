//package com.project.gugumarket.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "REPORTS")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class Report {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "REPORT_ID")
//    private Long reportId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "PRODUCT_ID", nullable = false)
//    private Product product;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "REPORTER_ID", nullable = false)
//    private User reporter;
//
//    @Column(name = "REASON", length = 500)
//    private String reason;
//
//    @CreationTimestamp
//    @Column(name = "CREATED_DATE")
//    private LocalDateTime createdDate;
//}

package com.project.gugumarket.entity;

import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "REPORTS")
// 🎯🔥 DB 레벨 중복 방지 추가 🔥🎯
//@Table(name = "REPORTS", uniqueConstraints = {
//        @UniqueConstraint(columnNames = {"PRODUCT_ID", "REPORTER_ID"})
//})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REPORT_ID")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_ID", nullable = false)
    private User reporter;

    @Column(name = "REASON", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    // 👇 추가!
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // 👇 Enum 추가!
    public enum ReportStatus {
        PENDING,   // 처리 대기
        RESOLVED   // 처리 완료
    }
}