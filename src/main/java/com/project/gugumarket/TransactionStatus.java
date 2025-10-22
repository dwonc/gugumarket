package com.project.gugumarket;

public enum TransactionStatus {
    PENDING("입금대기"),      // 구매 후 입금 대기 중
    COMPLETED("거래완료"),    // 입금 확인 완료
    CANCELLED("거래취소");    // 거래 취소됨

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}