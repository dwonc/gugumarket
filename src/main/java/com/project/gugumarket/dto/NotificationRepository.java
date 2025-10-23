package com.project.gugumarket.repository;

import com.project.gugumarket.NotificationType;
import com.project.gugumarket.entity.Notification;
import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 모든 알림 조회 (최신순)
     */
    List<Notification> findByReceiverOrderByCreatedDateDesc(User receiver);

    /**
     * 사용자의 읽지 않은/읽은 알림 조회
     */
    List<Notification> findByReceiverAndIsRead(User receiver, Boolean isRead);

    /**
     * 사용자의 읽지 않은 알림 개수
     */
    long countByReceiverAndIsRead(User receiver, Boolean isRead);

    /**
     * 사용자의 알림 타입별 조회
     */
    List<Notification> findByReceiverAndType(User receiver, NotificationType type);

    /**
     * 사용자의 모든 알림 삭제
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver = :receiver")
    void deleteByReceiver(@Param("receiver") User receiver);

    /**
     * 특정 상품에 대한 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.product.productId = :productId ORDER BY n.createdDate DESC")
    List<Notification> findByProductId(@Param("productId") Long productId);

    /**
     * 특정 거래에 대한 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.transaction.transactionId = :transactionId ORDER BY n.createdDate DESC")
    List<Notification> findByTransactionId(@Param("transactionId") Long transactionId);
}