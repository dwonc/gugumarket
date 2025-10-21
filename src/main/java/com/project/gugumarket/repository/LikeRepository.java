package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserAndProduct(User user, Product product);

    Optional<Like> findByUserAndProduct(User user, Product product);


    long countByProduct(Product product);


    Page<Like> findAllByUser(User user, Pageable pageable);

    // (선택) 유저+상품 조합 개수(보통 0/1—exists로 대체 가능)
    // long countByUserAndProduct(User user, Product product);
}
