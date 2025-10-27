package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {

    private Long categoryId;
    private String name;
    private Long productCount;  // 상품 개수

    /**
     * Entity → DTO 변환 (기본)
     */
    public static CategoryDto fromEntity(Category category) {
        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .build();
    }

    /**
     * Entity → DTO 변환 (상품 개수 포함)
     */
    public static CategoryDto fromEntityWithCount(Category category, Long productCount) {
        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .productCount(productCount)
                .build();
    }
}
