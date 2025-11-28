package com.project.gugumarket.dto;

import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimpleDto {
    private Long productId;
    private String title;           // ✅ productName → title
    private Integer price;
    private ProductStatus status;   // ✅ String → ProductStatus enum
    private String categoryName;    // ✅ Category 객체에서 이름만 추출
    private String mainImage;       // ✅ thumbnail → mainImage
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Boolean isDeleted;
    private Integer viewCount;
    private Integer likeCount;      // ✅ likes.size()로 계산
    private String seller;

    // ✅ seller, category 엔티티 제외! (순환 참조 방지)

    public static ProductSimpleDto fromEntity(Product product) {
        return ProductSimpleDto.builder()
                .productId(product.getProductId())
                .seller(product.getSeller().getNickname())
                .title(product.getTitle())
                .price(product.getPrice())
                .status(product.getStatus())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .mainImage(product.getMainImage())
                .createdDate(product.getCreatedDate())
                .updatedDate(product.getUpdatedDate())
                .isDeleted(product.getIsDeleted())
                .viewCount(product.getViewCount())
                .likeCount(product.getLikes() != null ? product.getLikes().size() : 0)
                .build();
    }
}