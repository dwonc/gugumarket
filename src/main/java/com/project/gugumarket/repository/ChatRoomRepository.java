package com.project.gugumarket.repository;

import com.project.gugumarket.entity.ChatRoom;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 Repository
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 상품 + 구매자로 채팅방 찾기 (유니크 제약조건)
     */
    Optional<ChatRoom> findByProductAndBuyer(Product product, User buyer);

    /**
     * 상품 ID + 구매자 ID로 채팅방 찾기
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.product.productId = :productId " +
            "AND cr.buyer.userId = :buyerId")
    Optional<ChatRoom> findByProductIdAndBuyerId(
            @Param("productId") Long productId,
            @Param("buyerId") Long buyerId
    );

    /**
     * 판매자의 모든 채팅방 조회 (최신순)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.seller.userId = :sellerId " +
            "ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoom> findBySellerIdOrderByLastMessageAtDesc(@Param("sellerId") Long sellerId);

    /**
     * 구매자의 모든 채팅방 조회 (최신순)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.buyer.userId = :buyerId " +
            "ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoom> findByBuyerIdOrderByLastMessageAtDesc(@Param("buyerId") Long buyerId);

    /**
     * 사용자의 모든 채팅방 조회 (판매자 + 구매자)
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.seller.userId = :userId OR cr.buyer.userId = :userId " +
            "ORDER BY cr.lastMessageAt DESC NULLS LAST, cr.createdAt DESC")
    List<ChatRoom> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);

    /**
     * 판매자의 총 읽지 않은 메시지 수
     */
    @Query("SELECT COALESCE(SUM(cr.sellerUnreadCount), 0) FROM ChatRoom cr " +
            "WHERE cr.seller.userId = :sellerId")
    Integer countTotalUnreadBySellerId(@Param("sellerId") Long sellerId);

    /**
     * 구매자의 총 읽지 않은 메시지 수
     */
    @Query("SELECT COALESCE(SUM(cr.buyerUnreadCount), 0) FROM ChatRoom cr " +
            "WHERE cr.buyer.userId = :buyerId")
    Integer countTotalUnreadByBuyerId(@Param("buyerId") Long buyerId);

    /**
     * 사용자의 총 읽지 않은 메시지 수 (판매자 + 구매자)
     */
    @Query("SELECT COALESCE(SUM(CASE " +
            "WHEN cr.seller.userId = :userId THEN cr.sellerUnreadCount " +
            "WHEN cr.buyer.userId = :userId THEN cr.buyerUnreadCount " +
            "ELSE 0 END), 0) " +
            "FROM ChatRoom cr " +
            "WHERE cr.seller.userId = :userId OR cr.buyer.userId = :userId")
    Integer countTotalUnreadByUserId(@Param("userId") Long userId);

    /**
     * 상품의 모든 채팅방 조회
     */
    List<ChatRoom> findByProduct(Product product);
}