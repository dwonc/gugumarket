package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 정보를 클라이언트에게 전달하기 위한 DTO (Data Transfer Object)
 * Entity를 직접 노출하지 않고 필요한 데이터만 선택적으로 전송
 */
@Getter // 모든 필드에 대한 Getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용한 객체 생성 지원
@NoArgsConstructor // 인자가 없는 기본 생성자를 자동으로 생성
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 자동으로 생성
public class NotificationResponseDto {
    // 알림 고유 ID
    private Long notificationId;
    // 알림 메시지 내용
    private String message;
    // 알림 타입 (댓글, 좋아요, 거래 등을 문자열로 표현)
    private String type;  // NotificationType → String
    // 알림 읽음 여부
    private Boolean isRead;
    // 알림 클릭 시 이동할 URL (관련 페이지 링크)
    private String url;  // relatedUrl → url로 수정
    // 알림이 생성된 날짜 및 시간
    private LocalDateTime createdDate;
    // 알림을 읽은 날짜 및 시간 (읽지 않은 경우 null)
    private LocalDateTime readDate;  // ✅ 추가

    /**
     * Notification 엔티티를 NotificationResponseDto로 변환하는 정적 팩토리 메서드
     *
     * @param notification 변환할 Notification 엔티티
     * @return 변환된 NotificationResponseDto 객체
     */
    public static NotificationResponseDto fromEntity(Notification notification) {
        // 빌더 패턴을 사용하여 DTO 객체 생성 및 반환
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId()) // 알림 ID 설정
                .message(notification.getMessage()) // 알림 메시지 설정
                .type(notification.getType().name())  // Enum을 문자열로 변환하여 설정
                .isRead(notification.getIsRead()) // 읽음 여부 설정
                .url(notification.getUrl())  // 관련 URL 설정 (relatedUrl → url)
                .createdDate(notification.getCreatedDate()) // 생성 날짜 설정
                .readDate(notification.getReadDate())  // ✅ 읽은 날짜 설정 (null 가능)
                .build(); // DTO 객체 생성
    }
}