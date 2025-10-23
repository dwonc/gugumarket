package com.project.gugumarket.service;

import com.project.gugumarket.NotificationType;
import com.project.gugumarket.entity.*;
import com.project.gugumarket.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 찜 알림 생성
     * - 판매자에게 누가 상품을 찜했는지 알림
     */
    @Transactional
    public Notification createLikeNotification(Like like) {
        User seller = like.getProduct().getSeller();
        User liker = like.getUser();
        Product product = like.getProduct();

        // 자기 상품을 찜한 경우 알림 생성 안함
        if (seller.getUserId().equals(liker.getUserId())) {
            log.info("자기 상품 찜 - 알림 생성하지 않음");
            return null;
        }

        String message = String.format("%s님이 '%s' 상품을 찜했습니다.",
                liker.getNickname(),
                product.getTitle());

        Notification notification = Notification.builder()
                .receiver(seller)
                .sender(liker)
                .product(product)
                .type(NotificationType.LIKE)
                .message(message)
                .url("/product/" + product.getProductId())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("찜 알림 생성 완료 - ID: {}, 판매자: {}, 찜한 사람: {}",
                saved.getNotificationId(), seller.getNickname(), liker.getNickname());

        return saved;
    }

    /**
     * 구매 알림 생성
     * - 판매자에게 누가 무엇을 구매했는지 알림
     */
    @Transactional
    public Notification createPurchaseNotification(Transaction transaction) {
        User seller = transaction.getSeller();
        User buyer = transaction.getBuyer();
        Product product = transaction.getProduct();

        String message = String.format("%s님이 '%s' 상품을 구매했습니다. (입금자명: %s)",
                buyer.getNickname(),
                product.getTitle(),
                transaction.getDepositorName() != null ? transaction.getDepositorName() : "미입력");

        Notification notification = Notification.builder()
                .receiver(seller)
                .sender(buyer)
                .product(product)
                .transaction(transaction)
                .type(NotificationType.PURCHASE)
                .message(message)
                .url("/transaction/" + transaction.getTransactionId())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("구매 알림 생성 완료 - ID: {}, 판매자: {}, 구매자: {}",
                saved.getNotificationId(), seller.getNickname(), buyer.getNickname());

        return saved;
    }

    /**
     * 거래 완료 알림 생성
     */
    @Transactional
    public Notification createTransactionCompleteNotification(Transaction transaction) {
        User seller = transaction.getSeller();
        User buyer = transaction.getBuyer();
        Product product = transaction.getProduct();

        String message = String.format("%s님과 '%s' 상품 거래가 완료되었습니다.",
                buyer.getNickname(),
                product.getTitle());

        Notification notification = Notification.builder()
                .receiver(seller)
                .sender(buyer)
                .product(product)
                .transaction(transaction)
                .type(NotificationType.TRANSACTION)
                .message(message)
                .url("/transaction/" + transaction.getTransactionId())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("거래 완료 알림 생성 완료 - ID: {}", saved.getNotificationId());

        return saved;
    }

    /**
     * 댓글 알림 생성
     */
    @Transactional
    public Notification createCommentNotification(User receiver, User commenter, Product product, String comment) {
        // 자기가 자기 상품에 댓글 단 경우 알림 생성 안함
        if (receiver.getUserId().equals(commenter.getUserId())) {
            return null;
        }

        String message = String.format("%s님이 '%s' 상품에 댓글을 남겼습니다: %s",
                commenter.getNickname(),
                product.getTitle(),
                comment.length() > 30 ? comment.substring(0, 30) + "..." : comment);

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(commenter)
                .product(product)
                .type(NotificationType.COMMENT)
                .message(message)
                .url("/product/" + product.getProductId())
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * 사용자의 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByReceiverAndIsRead(user, false);
    }

    /**
     * 사용자의 모든 알림 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByReceiverOrderByCreatedDateDesc(user);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림인지 확인
        if (!notification.getReceiver().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            log.info("알림 읽음 처리 완료 - ID: {}", notificationId);
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository
                .findByReceiverAndIsRead(user, false);

        unreadNotifications.forEach(Notification::markAsRead);
        log.info("모든 알림 읽음 처리 완료 - 사용자: {}, 개수: {}",
                user.getNickname(), unreadNotifications.size());
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림인지 확인
        if (!notification.getReceiver().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        notificationRepository.delete(notification);
        log.info("알림 삭제 완료 - ID: {}", notificationId);
    }

    /**
     * 모든 알림 삭제
     */
    @Transactional
    public void deleteAllNotifications(User user) {
        notificationRepository.deleteByReceiver(user);
        log.info("모든 알림 삭제 완료 - 사용자: {}", user.getNickname());
    }
}