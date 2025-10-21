package com.project.gugumarket.entity;

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

    @Column(name = "TYPE", length = 20, nullable = false)
    private String type;  // COMMENT, LIKE, PURCHASE, QNA_ANSWER 등

    @Column(name = "MESSAGE", length = 255, nullable = false)
    private String message;

    @Column(name = "URL", length = 255)
    private String url;

    @Column(name = "IS_READ")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
}