package com.project.gugumarket;

public enum NotificationType {
    LIKE("찜"),
    PURCHASE("구매"),
    COMMENT("댓글"),
    QNA_ANSWER("문의 답변"),
    TRANSACTION("거래 완료");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}