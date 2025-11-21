package com.project.gugumarket.dto;

import com.project.gugumarket.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private Long imageId;
    private String imageUrl;
    private Integer imageOrder;

    public static ProductImageDto from(ProductImage image) {
        return new ProductImageDto(
            image.getImageId(),
            image.getImageUrl(),
            image.getImageOrder()
        );
    }
}