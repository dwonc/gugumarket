package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductDto {
    private Long productId;
    private Long likeCount;

    public static ProductDto of(Long productId, long likeCount) {
        return ProductDto.builder()
                .productId(productId)
                .likeCount(likeCount)
                .build();
    }

    // 필요 시 확장: 제목/가격/썸네일 등을 추후 필드로 추가
    public static Long extractId(Product product) {
        try { return (Long) product.getClass().getMethod("getProductId").invoke(product); }
        catch (Exception ignored) {}
        try { return (Long) product.getClass().getMethod("getId").invoke(product); }
        catch (Exception ignored) {}
        throw new IllegalStateException("Product에 id 게터(getProductId 또는 getId)가 필요합니다.");
    }
}
