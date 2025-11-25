package com.project.gugumarket.entity;

import com.project.gugumarket.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter  // ⭐ 이거 추가!
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

    // ========== 기존 필드 ==========
    @Column(name = "depositor_name", length = 50)
    private String depositorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // ========== 결제 수단 필드 ==========
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    // ========== 카카오페이 기존 필드 ==========
    @Column(name = "tid", length = 100)
    private String tid;  // 카카오페이 거래번호

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;  // 결제 승인 시각

    @Column(name = "paid_amount")
    private Integer paidAmount;

    @Column(name = "payment_method_type", length = 20)
    private String paymentMethodType;  // CARD, MONEY 등

    // ========== 🆕 카카오페이 추가 필드 ==========
    @Column(name = "kakaopay_aid", length = 100)
    private String kakaoPayAid;  // 카카오페이 승인번호

    // ========== 🆕 거래 관리 필드 ==========
    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // 거래 완료 시각

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;  // 취소 시각

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;  // 취소 사유

    // ========== 🎬 정산 시뮬레이션 필드 ==========
    @Column(name = "settled")
    private Boolean settled = false;  // 정산 완료 여부

    @Column(name = "settled_at")
    private LocalDateTime settledAt;  // 정산 시각

    @Column(name = "settlement_amount")
    private Integer settlementAmount;  // 정산 금액

    // ========== 비즈니스 메서드 ==========

    /**
     * 입금자명 수정
     */
    public void updateDepositor(String depositorName) {
        this.depositorName = depositorName;
    }

    /**
     * 거래 취소
     */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    /**
     * 카카오페이 결제 승인 처리
     */
    public void approveKakaoPay(String tid, Integer paidAmount, String paymentMethodType) {
        this.tid = tid;
        this.paidAmount = paidAmount;
        this.paymentMethodType = paymentMethodType;
        this.approvedAt = LocalDateTime.now();
        this.status = TransactionStatus.COMPLETED;
    }

    /**
     * 무통장 입금 완료 처리
     */
    public void completeBankTransfer() {
        this.status = TransactionStatus.COMPLETED;
        this.transactionDate = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
        if (this.paymentMethod == null) {
            this.paymentMethod = "BANK_TRANSFER";
        }
    }
}