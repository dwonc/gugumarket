package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {
    private Long transactionId;
    private Long productId;
    private String productTitle;
    private Integer productPrice;
    private String productImage;
    private String buyerName;
    private String sellerName;
    private String depositorName;  // ✅ 추가
    private String status;
    private LocalDateTime transactionDate;
    private LocalDateTime createdDate;  // ✅ 추가


    public static TransactionResponseDto fromEntity(Transaction transaction) {
        Product product = transaction.getProduct();
        String imageUrl = product.getMainImage(); // ✅ 수정

        // 또는 첫 번째 productImages 사용
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrl = product.getProductImages().getFirst().getImageUrl();
        }
        return TransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .productId(transaction.getProduct().getProductId())
                .productTitle(transaction.getProduct().getTitle())
                .productPrice(transaction.getProduct().getPrice())
                .productImage(transaction.getProduct().getMainImage())
                .buyerName(transaction.getBuyer().getNickname())
                .sellerName(transaction.getSeller().getNickname())
                .depositorName(transaction.getDepositorName())  // ✅ 추가
                .status(transaction.getStatus().name())
                .transactionDate(transaction.getTransactionDate())
                .createdDate(transaction.getCreatedDate())  // ✅ 추가
                .build();
    }
}