package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 삭제되지 않은 상품을 최신순으로 페이징 조회
     */
    Page<Product> findByIsDeletedFalseOrderByCreatedDateDesc(Pageable pageable);

    /**
     * 카테고리별 상품 조회 (페이징)
     */
    Page<Product> findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(Long categoryId, Pageable pageable);

    /**
     * 제목으로 검색 (전체 카테고리)
     */
    Page<Product> findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(String keyword, Pageable pageable);

    /**
     * 제목 + 카테고리로 검색
     */
    Page<Product> findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
            String keyword, Long categoryId, Pageable pageable);
}