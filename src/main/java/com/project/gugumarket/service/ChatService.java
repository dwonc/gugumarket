package com.project.gugumarket.service;

import com.project.gugumarket.dto.chat.*;
import com.project.gugumarket.entity.*;
import com.project.gugumarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 채팅 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * 채팅방 생성 또는 조회
     * - 이미 존재하는 채팅방이면 조회
     * - 없으면 새로 생성
     */
    @Transactional
    public ChatRoomDto createOrGetChatRoom(Long productId, Long buyerId) {
        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        // 구매자 조회
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ✅ 판매자 조회 (수정: getUser() → getSeller())
        User seller = product.getSeller();

        // 자기 자신과는 채팅 불가
        if (seller.getUserId().equals(buyerId)) {
            throw new RuntimeException("자신의 상품과는 채팅할 수 없습니다.");
        }

        // 기존 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByProductAndBuyer(product, buyer)
                .orElseGet(() -> {
                    // 새 채팅방 생성
                    ChatRoom newChatRoom = ChatRoom.builder()
                            .product(product)
                            .seller(seller)
                            .buyer(buyer)
                            .sellerUnreadCount(0)
                            .buyerUnreadCount(0)
                            .build();
                    return chatRoomRepository.save(newChatRoom);
                });

        return ChatRoomDto.fromEntity(chatRoom);
    }

    /**
     * 사용자의 채팅방 목록 조회
     */
    public List<ChatRoomDto> getChatRoomList(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        return chatRooms.stream()
                .map(ChatRoomDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 조회
     */
    public ChatRoomDto getChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 권한 확인 (판매자 또는 구매자만 접근 가능)
        if (!chatRoom.getSeller().getUserId().equals(userId) &&
                !chatRoom.getBuyer().getUserId().equals(userId)) {
            throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
        }

        return ChatRoomDto.fromEntity(chatRoom);
    }

    /**
     * 채팅방의 메시지 목록 조회
     */
    public List<ChatMessageDto> getMessages(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 권한 확인
        if (!chatRoom.getSeller().getUserId().equals(userId) &&
                !chatRoom.getBuyer().getUserId().equals(userId)) {
            throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
        return messages.stream()
                .map(ChatMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public ChatMessageDto sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 권한 확인
        if (!chatRoom.getSeller().getUserId().equals(senderId) &&
                !chatRoom.getBuyer().getUserId().equals(senderId)) {
            throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
        }

        // 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(request.getMessageType())
                .content(request.getContent())
                .isRead(false)
                .build();

        message = chatMessageRepository.save(message);

        // 채팅방 정보 업데이트
        chatRoom.setLastMessage(request.getContent());
        chatRoom.setLastMessageAt(LocalDateTime.now());

        // 읽지 않은 메시지 수 증가
        boolean isSeller = chatRoom.getSeller().getUserId().equals(senderId);
        chatRoom.incrementUnreadCount(!isSeller);  // 상대방의 읽지 않은 메시지 수 증가

        chatRoomRepository.save(chatRoom);

        return ChatMessageDto.fromEntity(message);
    }

    /**
     * 메시지 읽음 처리
     */
    @Transactional
    public void markMessagesAsRead(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 권한 확인
        if (!chatRoom.getSeller().getUserId().equals(userId) &&
                !chatRoom.getBuyer().getUserId().equals(userId)) {
            throw new RuntimeException("채팅방에 접근할 권한이 없습니다.");
        }

        // 메시지 읽음 처리
        chatMessageRepository.markAllAsRead(chatRoomId, userId);

        // 읽지 않은 메시지 수 초기화
        boolean isSeller = chatRoom.getSeller().getUserId().equals(userId);
        chatRoom.resetUnreadCount(isSeller);

        chatRoomRepository.save(chatRoom);
    }

    /**
     * 사용자의 총 읽지 않은 메시지 수 조회
     */
    public Integer getTotalUnreadCount(Long userId) {
        return chatRoomRepository.countTotalUnreadByUserId(userId);
    }

    /**
     * 채팅방 삭제 (관리자 전용 또는 양측 동의)
     */
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 권한 확인
        if (!chatRoom.getSeller().getUserId().equals(userId) &&
                !chatRoom.getBuyer().getUserId().equals(userId)) {
            throw new RuntimeException("채팅방을 삭제할 권한이 없습니다.");
        }

        // 메시지 먼저 삭제
        chatMessageRepository.deleteByChatRoom(chatRoom);

        // 채팅방 삭제
        chatRoomRepository.delete(chatRoom);
    }
    // ChatService.java - createOrGetChatRoomWithUser 메서드 수정

    /**
     * 특정 사용자와 채팅방 생성 또는 조회 (거래용)
     */
    @Transactional
    public ChatRoomDto createOrGetChatRoomWithUser(Long productId, Long userId, Long otherUserId) {
        log.info("=== createOrGetChatRoomWithUser 시작 ===");
        log.info("productId: {}, userId: {}, otherUserId: {}", productId, userId, otherUserId);

        // 1. Product 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        // 2. 두 사용자 조회
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("상대방을 찾을 수 없습니다."));

        // 3. 기존 채팅방 찾기 (양방향)
        Optional<ChatRoom> existingChatRoom = chatRoomRepository
                .findByProductAndBuyerAndSeller(product, currentUser, otherUser);

        if (existingChatRoom.isEmpty()) {
            existingChatRoom = chatRoomRepository
                    .findByProductAndBuyerAndSeller(product, otherUser, currentUser);
        }

        if (existingChatRoom.isPresent()) {
            log.info("✅ 기존 채팅방 발견: {}", existingChatRoom.get().getChatRoomId());
            return ChatRoomDto.fromEntity(existingChatRoom.get());
        }

        // 4. 새 채팅방 생성
        log.info("✅ 새 채팅방 생성");

        User seller = product.getSeller();
        User buyer = seller.getUserId().equals(userId) ? otherUser : currentUser;

        ChatRoom newChatRoom = ChatRoom.builder()
                .product(product)
                .seller(seller)
                .buyer(buyer)
                .build();

        chatRoomRepository.save(newChatRoom);

        log.info("✅ 채팅방 생성 성공: {}", newChatRoom.getChatRoomId());

        return ChatRoomDto.fromEntity(newChatRoom);
    }

}