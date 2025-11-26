// ========================================
// ChatRoomCreateRequest.java
// ========================================
package com.project.gugumarket.dto.chat;

import lombok.*;

/**
 * 채팅방 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomCreateRequest {

    private Long productId;
}