package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 🔥 특정 상품을 좋아요한 모든 Like 조회
    List<Like> findByProduct_ProductId(Long productId);

    // 특정 사용자가 특정 상품을 좋아요 했는지 확인
    Optional<Like> findByUserAndProduct(User user, Product product);

    // 특정 사용자가 좋아요한 모든 목록
    List<Like> findByUserOrderByCreatedDateDesc(User user);

    // 특정 상품의 좋아요 개수
    Long countByProduct(Product product);

    // 사용자가 해당 상품을 좋아요 했는지 확인
    boolean existsByUserAndProduct(User user, Product product);
}