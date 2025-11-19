package com.project.gugumarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore  // ✅ 추가
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_ID")
    @JsonIgnore  // ✅ 추가
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    @JsonIgnore  // ✅ 추가
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_ID")
    @JsonIgnore  // ✅ 추가
    private Transaction transaction;

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

    @Column(name = "READ_DATE")
    private LocalDateTime readDate;

    public void markAsRead() {
        this.isRead = true;
        this.readDate = LocalDateTime.now();
    }
}