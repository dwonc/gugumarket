// ========================================
// ChatRoomDto.java
// ========================================
package com.project.gugumarket.dto.chat;

import com.project.gugumarket.entity.ChatRoom;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 채팅방 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long chatRoomId;

    // 상품 정보
    private Long productId;
    private String productTitle;
    private String productImage;
    private Integer productPrice;
    private String productStatus;

    // 판매자 정보
    private Long sellerId;
    private String sellerNickname;
    private String sellerProfileImage;

    // 구매자 정보
    private Long buyerId;
    private String buyerNickname;
    private String buyerProfileImage;

    // 채팅방 정보
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer sellerUnreadCount;
    private Integer buyerUnreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static ChatRoomDto fromEntity(ChatRoom chatRoom) {
        return ChatRoomDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .productId(chatRoom.getProduct().getProductId())
                .productTitle(chatRoom.getProduct().getTitle())
                .productImage(chatRoom.getProduct().getMainImage())
                .productPrice(chatRoom.getProduct().getPrice())
                .productStatus(chatRoom.getProduct().getStatus().name())
                .sellerId(chatRoom.getSeller().getUserId())
                .sellerNickname(chatRoom.getSeller().getNickname())
                .sellerProfileImage(chatRoom.getSeller().getProfileImage())
                .buyerId(chatRoom.getBuyer().getUserId())
                .buyerNickname(chatRoom.getBuyer().getNickname())
                .buyerProfileImage(chatRoom.getBuyer().getProfileImage())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .sellerUnreadCount(chatRoom.getSellerUnreadCount())
                .buyerUnreadCount(chatRoom.getBuyerUnreadCount())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
}
