package com.project.gugumarket.entity;


import com.project.gugumarket.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    // 🔥 이 필드들 추가!
    @Column(name = "depositor_name", length = 50)
    private String depositorName;  // 입금자명

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;  // PENDING, COMPLETED, CANCELLED

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // 🔥 입금자명 수정 메서드
    public void updateDepositor(String depositorName) {
        this.depositorName = depositorName;
    }

    // 🔥 거래 취소 메서드
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
    }
}