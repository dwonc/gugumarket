package com.project.gugumarket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 채팅방 엔티티
 * - 하나의 상품에 대해 판매자와 구매자 간의 1:1 채팅방
 * - productId + buyerId 조합으로 유니크한 채팅방 생성
 */
@Entity
@Table(name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "buyer_id"})
        },
        indexes = {
                @Index(name = "idx_seller_id", columnList = "seller_id"),
                @Index(name = "idx_buyer_id", columnList = "buyer_id"),
                @Index(name = "idx_product_id", columnList = "product_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    /**
     * 상품 ID (어떤 상품에 대한 채팅인지)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 판매자 (상품 판매자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /**
     * 구매자 (상품에 관심 있는 구매자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /**
     * 마지막 메시지 내용
     */
    @Column(name = "last_message", length = 500)
    private String lastMessage;

    /**
     * 마지막 메시지 시간
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 판매자 읽지 않은 메시지 수
     */
    @Column(name = "seller_unread_count", nullable = false)
    @Builder.Default
    private Integer sellerUnreadCount = 0;

    /**
     * 구매자 읽지 않은 메시지 수
     */
    @Column(name = "buyer_unread_count", nullable = false)
    @Builder.Default
    private Integer buyerUnreadCount = 0;

    /**
     * 채팅방 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 채팅방 수정 시간
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.sellerUnreadCount == null) {
            this.sellerUnreadCount = 0;
        }
        if (this.buyerUnreadCount == null) {
            this.buyerUnreadCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 읽지 않은 메시지 수 증가
     */
    public void incrementUnreadCount(boolean isSeller) {
        if (isSeller) {
            this.sellerUnreadCount++;
        } else {
            this.buyerUnreadCount++;
        }
    }

    /**
     * 읽지 않은 메시지 수 초기화
     */
    public void resetUnreadCount(boolean isSeller) {
        if (isSeller) {
            this.sellerUnreadCount = 0;
        } else {
            this.buyerUnreadCount = 0;
        }
    }
}