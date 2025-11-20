package com.project.gugumarket.dto;

import java.time.LocalDateTime;

import com.project.gugumarket.entity.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductDetailResponse {

  //상품 기본 정보
  private Long productId;
  private String title;
  private Integer price;
  private String content;
  private String mainImage;
  private Integer viewCount;
  private String status;  // AVAILABLE, RESERVED, SOLD_OUT
  private LocalDateTime createdDate;
  private LocalDateTime modifiedDate;

  //계좌 정보
  private String bankName;
  private String accountNumber;
  private String accountHolder;

  //판매자 정보 (중첩 객체 대신 필요한 필드만)
  private Long sellerId;
  private String sellerUsername;
  private String sellerNickname;

  //카테고리 정보
  private Long categoryId;
  private String categoryName;


    /**
     * 🔄 Entity → DTO 변환 메서드 (정적 팩토리 메서드)
     * 
     * @param product Entity 객체
     * @return DTO 객체
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
      dto.status = product.getStatus().name();  // Enum → String
      dto.createdDate = product.getCreatedDate();
      dto.modifiedDate = product.getUpdatedDate();


     // 계좌 정보
      dto.bankName = product.getBankName();
      dto.accountNumber = product.getAccountNumber();
      dto.accountHolder = product.getAccountHolder();
        
      // 판매자 정보 (User Entity가 아닌 필요한 필드만 추출)
      if (product.getSeller() != null) {
            dto.sellerId = product.getSeller().getUserId();
            dto.sellerUsername = product.getSeller().getUserName();
            dto.sellerNickname = product.getSeller().getNickname();
          }
        
      // 카테고리 정보
      if (product.getCategory() != null) {
            dto.categoryId = product.getCategory().getCategoryId();
            dto.categoryName = product.getCategory().getName();
          }
        
        return dto;
    }
}

