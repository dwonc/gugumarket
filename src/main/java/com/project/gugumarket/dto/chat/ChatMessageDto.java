package com.project.gugumarket.dto.chat;

import com.project.gugumarket.entity.ChatMessage;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long messageId;
    private Long chatRoomId;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImage;
    private ChatMessage.MessageType messageType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static ChatMessageDto fromEntity(ChatMessage message) {
        return ChatMessageDto.builder()
                .messageId(message.getMessageId())
                .chatRoomId(message.getChatRoom().getChatRoomId())
                .senderId(message.getSender().getUserId())
                .senderNickname(message.getSender().getNickname())
                .senderProfileImage(message.getSender().getProfileImage())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}




