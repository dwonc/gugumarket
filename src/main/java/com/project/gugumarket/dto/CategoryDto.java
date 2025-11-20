package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카테고리 정보 DTO
 * Entity의 순환 참조를 방지하고 필요한 정보만 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private Long categoryId;
    private String categoryName;  // Category.name -> categoryName으로 매핑

    // 통계 정보 (선택적)
    private Integer productCount;  // 해당 카테고리의 상품 개수

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static CategoryDto fromEntity(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getName())  // getName() 사용
                .productCount(0)  // 기본값, 필요시 Service에서 설정
                .build();
    }

    /**
     * Entity를 DTO로 변환 (상품 개수 포함)
     */
    public static CategoryDto fromEntityWithCount(Category category, int productCount) {
        CategoryDto dto = fromEntity(category);
        if (dto != null) {
            dto.setProductCount(productCount);
        }
        return dto;
    }
}
