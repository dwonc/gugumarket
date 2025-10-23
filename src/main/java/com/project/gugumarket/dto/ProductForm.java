package com.project.gugumarket.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductForm {

    private Long productId;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Long categoryId;

    @NotBlank(message = "상품명을 입력해주세요.")
    @Size(max = 100, message = "상품명은 100자 이내로 입력해주세요.")
    private String title;

    @NotNull(message = "가격을 입력해주세요.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotBlank(message = "상품 설명을 입력해주세요.")
    private String content;

    private String mainImage;

    // 추가 이미지 URL 리스트
    private List<String> additionalImages;

    // 계좌 정보
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 화면 표시용 필드 (유효성 검사 없음)
    private Integer viewCount;
    private LocalDateTime createdDate;
    private String sellerNickname;
    private String sellerAddress;
    private String categoryName;

    private Boolean isLiked = false;  // 현재 사용자의 찜 여부


    /**
     * Product 엔티티 → ProductForm 변환 (화면 표시용)
     */
    public static ProductForm fromEntity(com.project.gugumarket.entity.Product product) {
        if (product == null) {
            return null;
        }

        return ProductForm.builder()
                .productId(product.getProductId())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "미분류")
                .title(product.getTitle())
                .price(product.getPrice())
                .content(product.getContent())
                .mainImage(product.getMainImage())
                .bankName(product.getBankName())
                .accountNumber(product.getAccountNumber())
                .accountHolder(product.getAccountHolder())
                .viewCount(product.getViewCount() != null ? product.getViewCount() : 0)
                .createdDate(product.getCreatedDate())
                .sellerNickname(product.getSeller() != null ? product.getSeller().getNickname() : "알 수 없음")
                .sellerAddress(product.getSeller() != null ? product.getSeller().getAddress() : "위치 정보 없음")
                .build();
    }
}