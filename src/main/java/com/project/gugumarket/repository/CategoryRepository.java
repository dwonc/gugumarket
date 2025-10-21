package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 모든 카테고리 조회 (ID 순서대로)
     */
    List<Category> findAllByOrderByCategoryIdAsc();

    /**
     * 카테고리명으로 조회
     */
    Optional<Category> findByName(String name);

    /**
     * 카테고리명 중복 체크
     */
    boolean existsByName(String name);

    /**
     * 카테고리별 상품 개수 조회 (삭제되지 않은 상품만)
     */
    @Query("SELECT COUNT(p) FROM Product p " +
            "WHERE p.category.categoryId = :categoryId " +
            "AND p.isDeleted = false")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}

