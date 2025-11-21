package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Product 엔티티의 데이터베이스 접근을 담당하는 Repository
 * Spring Data JPA를 활용한 자동 쿼리 메서드 및 커스텀 쿼리 정의
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== 페이징 조회 메서드 (REST API용) ==========

    /**
     * 삭제되지 않은 상품을 최신순으로 페이징 조회
     */
    Page<Product> findByIsDeletedFalseOrderByCreatedDateDesc(Pageable pageable);

    /**
     * 카테고리별 상품 조회 (페이징, 삭제되지 않은 것만)
     */
    Page<Product> findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
            Long categoryId, Pageable pageable);

    /**
     * 제목으로 검색 (전체 카테고리, 페이징)
     */
    Page<Product> findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(
            String keyword, Pageable pageable);

    /**
     * 제목 + 카테고리로 검색 (페이징)
     */
    Page<Product> findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
            String keyword, Long categoryId, Pageable pageable);

    // ========== 🔥 REST API용 추가 메서드 ==========

    /**
     * 카테고리별 상품 조회 (페이징) - 간소화된 메서드명
     */
    Page<Product> findByCategoryCategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    /**
     * 카테고리 + 검색어로 상품 조회 (페이징)
     */
    Page<Product> findByCategoryCategoryIdAndTitleContainingAndIsDeletedFalse(
            Long categoryId, String title, Pageable pageable);

    /**
     * 상품명으로 검색 (페이징)
     */
    Page<Product> findByTitleContainingAndIsDeletedFalse(String title, Pageable pageable);

    /**
     * 카테고리별 상품 개수 조회
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.isDeleted = false")
    int countByCategoryCategoryId(@Param("categoryId") Long categoryId);

    // ========== 리스트 조회 메서드 ==========

    /**
     * 전체 상품 조회 (최신순)
     */
    List<Product> findAllByOrderByCreatedDateDesc();

    /**
     * 판매자별 상품 조회
     */
    List<Product> findBySellerUserId(Long userId);

    /**
     * 판매자별 상품 조회 (최신순)
     */
    List<Product> findBySellerUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * 판매자별 상품 조회 (페이징)
     */
    Page<Product> findBySellerUserIdAndIsDeletedFalseOrderByCreatedDateDesc(
            Long userId, Pageable pageable);

    /**
     * 상품 검색 (제목 또는 내용)
     */
    List<Product> findByTitleContainingOrContentContaining(String title, String content);

    /**
     * 삭제 상태별 상품 조회 (최신순)
     */
    List<Product> findByIsDeletedOrderByCreatedDateDesc(Boolean isDeleted);

    // ========== 통계 및 집계 쿼리 ==========

    /**
     * 전체 상품 개수 (삭제되지 않은 것만)
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isDeleted = false")
    long countActiveProducts();

    /**
     * 판매자별 상품 개수
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.userId = :userId AND p.isDeleted = false")
    int countBySellerUserId(@Param("userId") Long userId);

    /**
     * 상품 상태별 개수 조회
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status AND p.isDeleted = false")
    long countByStatus(@Param("status") String status);

    // ========== 검색 개선 쿼리 (선택적) ==========

    /**
     * 제목 또는 내용으로 검색 (페이징)
     */
    @Query("SELECT p FROM Product p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "AND p.isDeleted = false ORDER BY p.createdDate DESC")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 카테고리 + 키워드로 검색 (제목 또는 내용, 페이징)
     */
    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "AND p.isDeleted = false ORDER BY p.createdDate DESC")
    Page<Product> searchByCategoryAndKeyword(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
