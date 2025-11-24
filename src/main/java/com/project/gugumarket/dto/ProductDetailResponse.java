package com.project.gugumarket.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.project.gugumarket.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Getter
@NoArgsConstructor
public class ProductDetailResponse {

    // 상품 기본 정보
    private Long productId;
    private String title;
    private Integer price;
    private String content;
    private String mainImage;
    private Integer viewCount;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // 계좌 정보
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // ✅ 판매자 정보 - 객체로 변경!
    private SellerInfo seller;

    // ✅ 하위 호환성을 위한 평면 필드 유지 (선택사항)
    private Long sellerId;
    private String sellerUsername;
    private String sellerNickname;

    // 카테고리 정보
    private Long categoryId;
    private String categoryName;

    // 추가 이미지
    private List<ProductImageDto> productImages;

    // ✅ 내부 클래스: 판매자 정보
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private Long userId;
        private String userName;
        private String nickname;
        private String address;
    }

    /**
     * Entity → DTO 변환
     */
    public static ProductDetailResponse from(Product product) {
        ProductDetailResponse dto = new ProductDetailResponse();

        // 상품 기본 정보
        dto.productId = product.getProductId();
        dto.title = product.getTitle();
        dto.price = product.getPrice();
        dto.content = product.getContent();
        dto.mainImage = product.getMainImage();
        dto.viewCount = product.getViewCount();
        dto.status = product.getStatus().name();
        dto.createdDate = product.getCreatedDate();
        dto.modifiedDate = product.getUpdatedDate();

        // 계좌 정보
        dto.bankName = product.getBankName();
        dto.accountNumber = product.getAccountNumber();
        dto.accountHolder = product.getAccountHolder();

        // ✅ 판매자 정보 - 객체로 매핑
        if (product.getSeller() != null) {
            // ✅ seller 객체 생성
            dto.seller = SellerInfo.builder()
                    .userId(product.getSeller().getUserId())
                    .userName(product.getSeller().getUserName())
                    .nickname(product.getSeller().getNickname())
                    .address(product.getSeller().getAddress())
                    .build();

            // 하위 호환성을 위한 평면 필드도 채움
            dto.sellerId = product.getSeller().getUserId();
            dto.sellerUsername = product.getSeller().getUserName();
            dto.sellerNickname = product.getSeller().getNickname();
        }

        // 카테고리 정보
        if (product.getCategory() != null) {
            dto.categoryId = product.getCategory().getCategoryId();
            dto.categoryName = product.getCategory().getName();
        }

        // 추가 이미지 변환
        if (product.getProductImages() != null) {
            dto.productImages = product.getProductImages().stream()
                    .map(ProductImageDto::from)
                    .collect(Collectors.toList());
        }

        return dto;
    }
}