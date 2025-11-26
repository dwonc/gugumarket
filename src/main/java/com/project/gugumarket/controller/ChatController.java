package com.project.gugumarket.controller;

import com.project.gugumarket.dto.chat.*;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.handler.ChatMessageHandler;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.CustomUserDetails;
import com.project.gugumarket.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 채팅 REST API 컨트롤러 (디버깅 버전)
 */
@Slf4j  // ✅ 로깅 추가
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ChatMessageHandler chatMessageHandler;

    /**
     * 채팅방 생성 또는 조회
     * POST /api/chat/rooms
     */
    @PostMapping("/rooms")
    public ResponseEntity<?> createOrGetChatRoom(@RequestBody Map<String, Long> request) {
        try {
            log.info("=== 채팅방 생성/조회 요청 시작 ===");

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            Long productId = request.get("productId");
            log.info("✅ 상품 ID: {}", productId);

            ChatRoomDto chatRoom = chatService.createOrGetChatRoom(productId, userId);

            log.info("✅ 채팅방 생성/조회 성공: {}", chatRoom.getChatRoomId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chatRoom", chatRoom);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 채팅방 생성/조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 채팅방 목록 조회
     * GET /api/chat/rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getChatRoomList() {
        try {
            log.info("=== 채팅방 목록 조회 시작 ===");

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            List<ChatRoomDto> chatRooms = chatService.getChatRoomList(userId);
            log.info("✅ 채팅방 목록 조회 성공: {}개", chatRooms.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chatRooms", chatRooms);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 채팅방 목록 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 채팅방 상세 조회
     * GET /api/chat/rooms/{chatRoomId}
     */
    @GetMapping("/rooms/{chatRoomId}")
    public ResponseEntity<?> getChatRoom(@PathVariable Long chatRoomId) {
        try {
            log.info("=== 채팅방 상세 조회 시작: {} ===", chatRoomId);

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            ChatRoomDto chatRoom = chatService.getChatRoom(chatRoomId, userId);
            log.info("✅ 채팅방 상세 조회 성공");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chatRoom", chatRoom);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 채팅방 상세 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 채팅방의 메시지 목록 조회
     * GET /api/chat/rooms/{chatRoomId}/messages
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long chatRoomId) {
        try {
            log.info("=== 메시지 목록 조회 시작: {} ===", chatRoomId);

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            List<ChatMessageDto> messages = chatService.getMessages(chatRoomId, userId);
            log.info("✅ 메시지 목록 조회 성공: {}개", messages.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 메시지 목록 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 메시지 읽음 처리
     * PATCH /api/chat/rooms/{chatRoomId}/read
     */
    @PatchMapping("/rooms/{chatRoomId}/read")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long chatRoomId) {
        try {
            log.info("=== 메시지 읽음 처리 시작: {} ===", chatRoomId);

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            chatService.markMessagesAsRead(chatRoomId, userId);
            log.info("✅ 메시지 읽음 처리 성공");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));

            chatMessageHandler.sendChatUnreadCount(user);   // ✔ 정상

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "메시지를 읽음 처리했습니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 메시지 읽음 처리 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 총 읽지 않은 메시지 수 조회
     * GET /api/chat/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getTotalUnreadCount() {
        try {
            log.info("=== 읽지 않은 메시지 수 조회 시작 ===");

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            Integer unreadCount = chatService.getTotalUnreadCount(userId);
            log.info("✅ 읽지 않은 메시지 수: {}", unreadCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 읽지 않은 메시지 수 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 채팅방 삭제
     * DELETE /api/chat/rooms/{chatRoomId}
     */
    @DeleteMapping("/rooms/{chatRoomId}")
    public ResponseEntity<?> deleteChatRoom(@PathVariable Long chatRoomId) {
        try {
            log.info("=== 채팅방 삭제 시작: {} ===", chatRoomId);

            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            chatService.deleteChatRoom(chatRoomId, userId);
            log.info("✅ 채팅방 삭제 성공");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "채팅방이 삭제되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 채팅방 삭제 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 현재 인증된 사용자의 ID 가져오기
     */
    private Long getCurrentUserId() {
        log.info("=== getCurrentUserId 시작 ===");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Authentication: {}", authentication);
        log.info("Authentication.isAuthenticated(): {}", authentication != null ? authentication.isAuthenticated() : "null");
        log.info("Authentication.getPrincipal(): {}", authentication != null ? authentication.getPrincipal() : "null");

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("❌ 인증 정보가 없거나 인증되지 않음!");
            throw new RuntimeException("인증이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        log.info("Principal class: {}", principal.getClass().getName());

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Long userId = userDetails.getUserId();
            log.info("✅ CustomUserDetails에서 userId 추출 성공: {}", userId);
            return userId;
        }

        log.error("❌ Principal이 CustomUserDetails가 아님: {}", principal.getClass().getName());
        throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 특정 사용자와 채팅방 생성 (거래용)
     * @param request productId, otherUserId
     * @return ChatRoomDto
     */
    @PostMapping("/rooms/with-user")
    public ResponseEntity<?> createChatRoomWithUser(@RequestBody Map<String, Long> request) {
        log.info("=== 특정 사용자와 채팅방 생성/조회 요청 시작 ===");

        try {
            Long userId = getCurrentUserId();
            log.info("✅ 현재 사용자 ID: {}", userId);

            Long productId = request.get("productId");
            Long otherUserId = request.get("otherUserId");

            log.info("✅ 상품 ID: {}, 상대방 ID: {}", productId, otherUserId);

            if (productId == null || otherUserId == null) {
                log.error("❌ productId 또는 otherUserId가 null입니다.");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "상품 ID와 상대방 ID가 필요합니다."));
            }

            if (userId.equals(otherUserId)) {
                log.error("❌ 자기 자신과는 채팅할 수 없습니다.");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "자기 자신과는 채팅할 수 없습니다."));
            }

            ChatRoomDto chatRoomDto = chatService.createOrGetChatRoomWithUser(productId, userId, otherUserId);

            log.info("✅ 채팅방 생성/조회 성공: {}", chatRoomDto.getChatRoomId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "chatRoom", chatRoomDto
            ));
        } catch (Exception e) {
            log.error("❌ 채팅방 생성/조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}