package com.project.gugumarket.dto;

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
public class NotificationResponseDto {
    private Long notificationId;
    private String message;
    private String type;  // NotificationType → String
    private Boolean isRead;
    private String url;  // relatedUrl → url로 수정
    private LocalDateTime createdDate;
    private LocalDateTime readDate;  // ✅ 추가

    public static NotificationResponseDto fromEntity(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .message(notification.getMessage())
                .type(notification.getType().name())  // Enum → String
                .isRead(notification.getIsRead())
                .url(notification.getUrl())  // relatedUrl → url
                .createdDate(notification.getCreatedDate())
                .readDate(notification.getReadDate())  // ✅ 추가
                .build();
    }
}