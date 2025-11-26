// repository/ProductRepository.java
package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== 기존 메서드들 유지 ==========
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    Page<Product> findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(Long categoryId, Pageable pageable);
    Page<Product> findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(String keyword, Pageable pageable);
    Page<Product> findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalse(String keyword, Long categoryId, Pageable pageable);
    Page<Product> findByCategory_CategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);
    Page<Product> findByCategory_CategoryIdAndTitleContainingAndIsDeletedFalse(Long categoryId, String title, Pageable pageable);
    Page<Product> findByTitleContainingAndIsDeletedFalse(String title, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId AND p.isDeleted = false")
    int countByCategoryCategoryId(@Param("categoryId") Long categoryId);

    List<Product> findAllByOrderByCreatedDateDesc();
    List<Product> findBySellerUserId(Long userId);
    List<Product> findBySellerUserIdOrderByCreatedDateDesc(Long userId);
    Page<Product> findBySellerUserIdAndIsDeletedFalseOrderByCreatedDateDesc(Long userId, Pageable pageable);
    List<Product> findByTitleContainingOrContentContaining(String title, String content);
    List<Product> findByIsDeletedOrderByCreatedDateDesc(Boolean isDeleted);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isDeleted = false")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.userId = :userId AND p.isDeleted = false")
    int countBySellerUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status AND p.isDeleted = false")
    long countByStatus(@Param("status") String status);

    @Query("SELECT p FROM Product p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.isDeleted = false ORDER BY p.createdDate DESC")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.categoryId = :categoryId AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.isDeleted = false ORDER BY p.createdDate DESC")
    Page<Product> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    List<Product> findBySellerAndIsDeletedFalseOrderByCreatedDateDesc(User seller);

    @Query("SELECT p FROM Product p WHERE p.seller.address LIKE %:district% AND p.isDeleted = false")
    Page<Product> findByDistrictAndIsDeletedFalse(@Param("district") String district, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.seller.address LIKE %:district% AND p.category.categoryId = :categoryId AND p.isDeleted = false")
    Page<Product> findByDistrictAndCategoryAndIsDeletedFalse(@Param("district") String district, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.seller.address LIKE %:district% AND p.title LIKE %:keyword% AND p.isDeleted = false")
    Page<Product> findByDistrictAndKeywordAndIsDeletedFalse(@Param("district") String district, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.seller.address LIKE %:district% AND p.category.categoryId = :categoryId AND p.title LIKE %:keyword% AND p.isDeleted = false")
    Page<Product> findByDistrictAndCategoryAndKeywordAndIsDeletedFalse(@Param("district") String district, @Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT CASE WHEN u.address LIKE '%구 %' THEN SUBSTRING(u.address, LOCATE('구', u.address) - LOCATE(' ', REVERSE(SUBSTRING(u.address, 1, LOCATE('구', u.address)))) + 1, LOCATE('구', u.address) - LOCATE(' ', REVERSE(SUBSTRING(u.address, 1, LOCATE('구', u.address)))) + 1) ELSE NULL END FROM User u WHERE u.address IS NOT NULL AND u.address LIKE '%구%' ORDER BY 1")
    List<String> findDistinctDistricts();

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL ORDER BY p.createdDate DESC")
    List<Product> findAllWithCoordinates();

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.latitude BETWEEN :minLat AND :maxLat AND p.longitude BETWEEN :minLng AND :maxLng ORDER BY p.createdDate DESC")
    List<Product> findProductsInBounds(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat, @Param("minLng") Double minLng, @Param("maxLng") Double maxLng);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND (p.latitude IS NULL OR p.longitude IS NULL)")
    List<Product> findProductsWithoutCoordinates();

    // ========== 🆕 가격 필터링 추가 ==========

    /**
     * 🔥 좌표가 있고 + 최대 가격 이하인 상품 조회
     */
    @Query("SELECT p FROM Product p " +
            "WHERE p.isDeleted = false " +
            "AND p.latitude IS NOT NULL " +
            "AND p.longitude IS NOT NULL " +
            "AND p.price <= :maxPrice " +
            "ORDER BY p.createdDate DESC")
    List<Product> findAllWithCoordinatesAndMaxPrice(@Param("maxPrice") Integer maxPrice);
}