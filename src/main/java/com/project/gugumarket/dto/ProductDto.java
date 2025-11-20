package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 정보 DTO
 * Entity의 무한 재귀 문제를 방지하고 필요한 정보만 선택적으로 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productId;
    private String productName;  // Product.title
    private Integer price;
    private String description;  // Product.content
    private ProductStatus status;

    // 카테고리 정보 (순환 참조 방지)
    private Long categoryId;
    private String categoryName;

    // 판매자 정보 (순환 참조 방지)
    private Long sellerId;
    private String sellerNickname;
    private String sellerProfileImage;

    // 이미지 정보
    private String thumbnailImageUrl;  // 대표 이미지 (mainImage)
    private List<String> imageUrls;    // 전체 이미지 목록 (productImages)

    // 상태 정보
    private Boolean isLiked;           // 찜 여부
    private Integer likeCount;         // 찜 개수
    private Integer viewCount;         // 조회수
    private Integer commentCount;      // 댓글 개수

    // 계좌 정보
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 시간 정보
    private LocalDateTime createdAt;   // Product.createdDate
    private LocalDateTime updatedAt;   // Product.updatedDate

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ProductDto fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        // ProductImage 리스트에서 imageUrl만 추출
        List<String> imageUrls = new ArrayList<>();
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrls = product.getProductImages().stream()
                    .map(img -> img.getImageUrl())
                    .collect(Collectors.toList());
        }

        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getTitle())  // title -> productName 매핑
                .price(product.getPrice())
                .description(product.getContent())  // content -> description 매핑
                .status(product.getStatus())

                // 카테고리 정보
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)

                // 판매자 정보 (seller 필드 사용)
                .sellerId(product.getSeller() != null ? product.getSeller().getUserId() : null)
                .sellerNickname(product.getSeller() != null ? product.getSeller().getNickname() : null)
                .sellerProfileImage(product.getSeller() != null ? product.getSeller().getProfileImage() : null)

                // 이미지 정보
                .thumbnailImageUrl(product.getMainImage())  // mainImage를 썸네일로
                .imageUrls(imageUrls)  // productImages 리스트

                // 초기값 설정 (컨트롤러에서 업데이트 가능)
                .isLiked(false)
                .likeCount(product.getLikes() != null ? product.getLikes().size() : 0)
                .viewCount(product.getViewCount() != null ? product.getViewCount() : 0)
                .commentCount(product.getComments() != null ? product.getComments().size() : 0)

                // 계좌 정보
                .bankName(product.getBankName())
                .accountNumber(product.getAccountNumber())
                .accountHolder(product.getAccountHolder())

                // 시간 정보
                .createdAt(product.getCreatedDate())  // createdDate -> createdAt
                .updatedAt(product.getUpdatedDate())  // updatedDate -> updatedAt
                .build();
    }

    /**
     * Entity를 DTO로 변환 (상세 정보 포함)
     * 찜 여부와 카운트 정보를 외부에서 주입받음
     */
    public static ProductDto fromEntityWithDetails(Product product, boolean isLiked, int likeCount, int commentCount) {
        ProductDto dto = fromEntity(product);
        if (dto != null) {
            dto.setIsLiked(isLiked);
            dto.setLikeCount(likeCount);
            dto.setCommentCount(commentCount);
        }
        return dto;
    }
}
