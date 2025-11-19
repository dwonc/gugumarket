package com.project.gugumarket.dto;

import com.project.gugumarket.NotificationType;
import com.project.gugumarket.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long notificationId;
    private NotificationType type;          // ✅ LIKE, PURCHASE, COMMENT 등
    private String typeDescription;         // ✅ "찜", "구매", "댓글" 등
    private String message;
    private String url;
    private Boolean isRead;
    private LocalDateTime createdDate;
    private LocalDateTime readDate;

    // 발신자 정보
    private String senderName;
    private String senderNickname;

    // 상품 정보
    private Long productId;
    private String productTitle;

    // 거래 정보
    private Long transactionId;

    public static NotificationDto fromEntity(Notification notification) {
        return NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .typeDescription(notification.getType().getDescription())  // ✅ 추가
                .message(notification.getMessage())
                .url(notification.getUrl())
                .isRead(notification.getIsRead())
                .createdDate(notification.getCreatedDate())
                .readDate(notification.getReadDate())
                // 발신자 정보
                .senderName(notification.getSender() != null ? notification.getSender().getUserName() : null)
                .senderNickname(notification.getSender() != null ? notification.getSender().getNickname() : null)
                // 상품 정보
                .productId(notification.getProduct() != null ? notification.getProduct().getProductId() : null)
                .productTitle(notification.getProduct() != null ? notification.getProduct().getTitle() : null)
                // 거래 정보
                .transactionId(notification.getTransaction() != null ? notification.getTransaction().getTransactionId() : null)
                .build();
    }
}