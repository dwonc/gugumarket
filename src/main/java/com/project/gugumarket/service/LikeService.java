package com.project.gugumarket.service;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j  // 🔥 로깅 추가
public class LikeService {

    private final LikeRepository likeRepository;
    @Lazy
    private final NotificationService notificationService;  // 🔥 알림 서비스 추가

    /**
     * 좋아요 추가
     */
    @Transactional
    public void addLike(User user, Product product) {
        // 이미 좋아요 했는지 확인
        if (likeRepository.existsByUserAndProduct(user, product)) {
            throw new IllegalStateException("이미 좋아요한 상품입니다.");
        }

        Like like = Like.builder()
                .user(user)
                .product(product)
                .build();

        likeRepository.save(like);

        // 🔥 찜 알림 생성
        try {
            notificationService.createLikeNotification(like);
            log.info("찜 알림 생성 완료 - 사용자: {}, 상품: {}",
                    user.getNickname(), product.getTitle());
        } catch (Exception e) {
            log.error("찜 알림 생성 실패 - 사용자: {}, 상품: {}, 오류: {}",
                    user.getNickname(), product.getTitle(), e.getMessage());
            // 알림 생성 실패해도 찜 기능은 정상 동작
        }
    }

    /**
     * 좋아요 취소
     */
    @Transactional
    public void removeLike(User user, Product product) {
        Like like = likeRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalStateException("좋아요하지 않은 상품입니다."));

        likeRepository.delete(like);
        log.info("찜 취소 완료 - 사용자: {}, 상품: {}", user.getNickname(), product.getTitle());
    }

    /**
     * 좋아요 토글 (있으면 삭제, 없으면 추가)
     */
    @Transactional
    public boolean toggleLike(User user, Product product) {
        if (likeRepository.existsByUserAndProduct(user, product)) {
            removeLike(user, product);
            return false; // 좋아요 취소됨
        } else {
            addLike(user, product);
            return true; // 좋아요 추가됨
        }
    }

    /**
     * 사용자가 해당 상품을 좋아요 했는지 확인
     */
    public boolean isLiked(User user, Product product) {
        return likeRepository.existsByUserAndProduct(user, product);
    }

    /**
     * 특정 상품의 좋아요 개수
     */
    public Long getLikeCount(Product product) {
        return likeRepository.countByProduct(product);
    }

    /**
     * 사용자가 좋아요한 상품 목록
     */
    public List<Like> getUserLikes(User user) {
        return likeRepository.findByUserOrderByCreatedDateDesc(user);
    }

    /**
     * 특정 상품을 좋아요한 사용자 목록 (구매 희망자)
     */
    public List<User> getUsersWhoLikedProduct(Long productId) {
        return likeRepository.findByProduct_ProductId(productId)
                .stream()
                .map(Like::getUser)
                .collect(Collectors.toList());
    }
}