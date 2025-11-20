package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Category 엔티티의 데이터베이스 접근을 담당하는 Repository
 * 카테고리 조회 및 통계 쿼리 제공
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ========== 기본 조회 메서드 ==========

    /**
     * 카테고리 ID 오름차순 정렬
     */
    List<Category> findAllByOrderByCategoryIdAsc();

    /**
     * 카테고리 이름으로 조회
     */
    Optional<Category> findByName(String name);

    /**
     * 카테고리 이름으로 존재 여부 확인
     */
    boolean existsByName(String name);

    // ========== 통계 및 집계 쿼리 ==========

    /**
     * 특정 카테고리의 상품 개수 조회 (삭제되지 않은 상품만)
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.isDeleted = false")
    Long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 상품이 있는 카테고리만 조회
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.products p WHERE p.isDeleted = false ORDER BY c.categoryId ASC")
    List<Category> findCategoriesWithProducts();

    /**
     * 카테고리별 상품 개수 조회 (Map 형태)
     * 결과: [{categoryId: 1, count: 10}, {categoryId: 2, count: 5}, ...]
     */
    @Query("SELECT c.categoryId as categoryId, COUNT(p) as productCount " +
            "FROM Category c LEFT JOIN c.products p ON p.isDeleted = false " +
            "GROUP BY c.categoryId ORDER BY c.categoryId ASC")
    List<Object[]> getCategoryProductCounts();

    // ========== 추가 편의 메서드 ==========

    /**
     * 카테고리 이름으로 대소문자 구분 없이 조회
     */
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    /**
     * 카테고리 이름에 키워드 포함된 것 검색
     */
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% ORDER BY c.categoryId ASC")
    List<Category> searchByName(@Param("keyword") String keyword);

    /**
     * 활성 상품이 있는 카테고리 개수
     */
    @Query("SELECT COUNT(DISTINCT c.categoryId) FROM Category c JOIN c.products p WHERE p.isDeleted = false")
    long countCategoriesWithActiveProducts();
}
