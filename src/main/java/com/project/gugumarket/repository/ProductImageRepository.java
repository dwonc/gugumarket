package com.project.gugumarket.repository;

import com.project.gugumarket.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 특정 상품의 이미지 목록 조회 (순서대로)
    List<ProductImage> findByProduct_ProductIdOrderByImageOrderAsc(Long productId);

    // 특정 상품의 이미지 삭제
    void deleteByProduct_ProductId(Long productId);
}