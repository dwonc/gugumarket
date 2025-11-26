// ========================================
// ChatMessageRequest.java
// ========================================
package com.project.gugumarket.dto.chat;

import com.project.gugumarket.entity.ChatMessage;
import com.project.gugumarket.entity.ChatMessage;
import lombok.*;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    private Long chatRoomId;
    private com.project.gugumarket.entity.ChatMessage.MessageType messageType;
    private String content;
}