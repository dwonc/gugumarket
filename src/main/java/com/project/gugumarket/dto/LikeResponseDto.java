package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDto {
    private Long likeId;
    private Long productId;
    private String productTitle;
    private Integer productPrice;
    private String productImage;
    private String productStatus;
    private LocalDateTime createdDate;

    public static LikeResponseDto fromEntity(Like like) {
        Product product = like.getProduct();
        String imageUrl = product.getMainImage(); // ✅ getFirstImageUrl() → getMainImage()로 변경

        // 또는 첫 번째 productImages 사용
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrl = product.getProductImages().getFirst().getImageUrl();
        }

        return LikeResponseDto.builder()
                .likeId(like.getLikeId())
                .productId(product.getProductId())
                .productTitle(product.getTitle())
                .productPrice(product.getPrice())
                .productImage(imageUrl)  // ✅ 수정
                .productStatus(product.getStatus().name())
                .createdDate(like.getCreatedDate())
                .build();
    }
}