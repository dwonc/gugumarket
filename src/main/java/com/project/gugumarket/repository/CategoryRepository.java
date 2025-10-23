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
     * 카테고리 ID 오름차순 정렬
     */
    List<Category> findAllByOrderByCategoryIdAsc();

    /**
     * 특정 카테고리의 상품 개수 조회
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.isDeleted = false")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 카테고리 이름으로 조회
     */
    Optional<Category> findByName(String name);
}