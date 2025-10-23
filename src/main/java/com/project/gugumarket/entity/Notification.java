package com.project.gugumarket.entity;

import com.project.gugumarket.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATIONS", indexes = {
        @Index(name = "idx_receiver_read", columnList = "RECEIVER_ID, IS_READ"),
        @Index(name = "idx_created_date", columnList = "CREATED_DATE DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECEIVER_ID", nullable = false)
    private User receiver;

    // 알림을 발생시킨 사용자 (구매자, 찜한 사람 등)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_ID")
    private User sender;

    // 관련 상품
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    // 관련 거래 (구매 알림인 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_ID")
    private Transaction transaction;

    // 알림 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "MESSAGE", length = 255, nullable = false)
    private String message;

    @Column(name = "URL", length = 255)
    private String url;

    @Column(name = "IS_READ")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    // 읽은 일시
    @Column(name = "READ_DATE")
    private LocalDateTime readDate;

    /**
     * 알림을 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readDate = LocalDateTime.now();
    }
}