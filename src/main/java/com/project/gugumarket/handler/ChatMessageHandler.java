package com.project.gugumarket.handler;

import com.project.gugumarket.dto.chat.ChatMessageDto;
import com.project.gugumarket.dto.chat.ChatMessageRequest;
import com.project.gugumarket.entity.ChatMessage;
import com.project.gugumarket.entity.ChatRoom;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ChatMessageRepository;
import com.project.gugumarket.repository.ChatRoomRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.CustomUserDetails;
import com.project.gugumarket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 핸들러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatService chatService; // ← getTotalUnreadCount용 서비스(이미 있을 것)

    /**
     * 메시지 전송
     * /app/chat/send
     */
    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageRequest request, Authentication authentication) {
        try {
            log.info("=== 메시지 전송 요청 수신 ===");
            log.info("ChatRoomId: {}", request.getChatRoomId());
            log.info("Content: {}", request.getContent());
            log.info("MessageType: {}", request.getMessageType());

            // 1. 사용자 확인
            Long userId = getUserIdFromAuthentication(authentication);
            log.info("✅ 현재 사용자 ID: {}", userId);

            User sender = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 채팅방 확인
            ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                    .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

            // 3. 권한 확인 (판매자 또는 구매자만 가능)
            if (!chatRoom.getSeller().getUserId().equals(userId) &&
                    !chatRoom.getBuyer().getUserId().equals(userId)) {
                log.error("❌ 채팅방 접근 권한 없음: userId={}, chatRoomId={}", userId, request.getChatRoomId());
                throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
            }

            // 4. 메시지 저장
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .messageType(request.getMessageType())
                    .content(request.getContent())
                    .isRead(false)
                    .build();

            message = chatMessageRepository.save(message);
            log.info("✅ 메시지 저장 완료: messageId={}", message.getMessageId());

            // 5. 채팅방 정보 업데이트
            chatRoom.setLastMessage(request.getContent());
            chatRoom.setLastMessageAt(LocalDateTime.now());

            // ✅ 상대방 찾기
            boolean isSeller = chatRoom.getSeller().getUserId().equals(userId);
            User receiver = isSeller ? chatRoom.getBuyer() : chatRoom.getSeller();
            chatRoom.incrementUnreadCount(!isSeller);

            // ✅ 순서 변경: 먼저 저장!
            chatRoomRepository.save(chatRoom);
            log.info("✅ 채팅방 정보 업데이트 완료");

            // ✅ 저장 후에 카운트 전송
            sendChatUnreadCount(receiver);

            // 6. WebSocket으로 메시지 브로드캐스트
            ChatMessageDto messageDto = ChatMessageDto.fromEntity(message);
            String destination = "/topic/chat/" + request.getChatRoomId();

            messagingTemplate.convertAndSend(destination, messageDto);
            log.info("✅ 메시지 브로드캐스트 완료: destination={}", destination);

        } catch (Exception e) {
            log.error("❌ 메시지 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 전송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 채팅방 입장 알림
     * /app/chat/enter
     */
    @MessageMapping("/chat/enter")
    public void enterChatRoom(@Payload ChatMessageRequest request, Authentication authentication) {
        try {
            log.info("=== 채팅방 입장 알림 ===");
            log.info("ChatRoomId: {}", request.getChatRoomId());

            Long userId = getUserIdFromAuthentication(authentication);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            log.info("✅ {}님이 채팅방 {}에 입장했습니다.", user.getNickname(), request.getChatRoomId());

            // 시스템 메시지 전송 (선택사항)
            // ChatMessageDto systemMessage = ChatMessageDto.builder()
            //         .messageType("SYSTEM")
            //         .content(user.getNickname() + "님이 입장했습니다.")
            //         .createdAt(LocalDateTime.now())
            //         .build();
            //
            // String destination = "/topic/chat/" + request.getChatRoomId();
            // messagingTemplate.convertAndSend(destination, systemMessage);

        } catch (Exception e) {
            log.error("❌ 채팅방 입장 알림 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 채팅방 퇴장 알림
     * /app/chat/leave
     */
    @MessageMapping("/chat/leave")
    public void leaveChatRoom(@Payload ChatMessageRequest request, Authentication authentication) {
        try {
            log.info("=== 채팅방 퇴장 알림 ===");
            log.info("ChatRoomId: {}", request.getChatRoomId());

            Long userId = getUserIdFromAuthentication(authentication);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            log.info("✅ {}님이 채팅방 {}에서 퇴장했습니다.", user.getNickname(), request.getChatRoomId());

        } catch (Exception e) {
            log.error("❌ 채팅방 퇴장 알림 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * Authentication에서 userId 추출
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            return userDetails.getUserId();
        }

        throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
    }

    // ✅ 사용자별 총 안 읽은 채팅 수 실시간 전송
    public void sendChatUnreadCount(User targetUser) {
        try {
            long totalUnread = chatService.getTotalUnreadCount(targetUser.getUserId());
            String dest = "/topic/chat/unread-count/" + targetUser.getUserId();

            messagingTemplate.convertAndSend(dest, totalUnread);

            log.info("💬 실시간 채팅 unread 전송 완료 - userId: {}, dest: {}, count: {}",
                    targetUser.getUserId(), dest, totalUnread);
        } catch (Exception e) {
            log.error("❌ 채팅 unread 전송 실패: {}", e.getMessage(), e);
        }
    }

}