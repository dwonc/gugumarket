package com.project.gugumarket.repository;

import com.project.gugumarket.entity.ChatMessage;
import com.project.gugumarket.entity.ChatRoom;
import com.project.gugumarket.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방의 모든 메시지 조회 (시간순)
     */
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    /**
     * 채팅방의 최근 메시지 조회 (페이징)
     */
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoom.chatRoomId = :chatRoomId " +
            "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessages(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable
    );

    /**
     * 채팅방의 읽지 않은 메시지 수 조회
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
            "WHERE cm.chatRoom.chatRoomId = :chatRoomId " +
            "AND cm.sender.userId != :userId " +
            "AND cm.isRead = false")
    Integer countUnreadMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    /**
     * 채팅방의 모든 메시지를 읽음 처리
     */
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
            "WHERE cm.chatRoom.chatRoomId = :chatRoomId " +
            "AND cm.sender.userId != :userId " +
            "AND cm.isRead = false")
    int markAllAsRead(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    /**
     * 채팅방의 마지막 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm " +
            "WHERE cm.chatRoom.chatRoomId = :chatRoomId " +
            "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findLastMessage(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * 채팅방 삭제 시 메시지도 함께 삭제
     */
    void deleteByChatRoom(ChatRoom chatRoom);

}