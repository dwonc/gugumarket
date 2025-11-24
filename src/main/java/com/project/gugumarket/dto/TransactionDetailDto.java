package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Transaction;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailDto {

    private Long transactionId;
    private String status;
    private String depositorName;
    private LocalDateTime transactionDate;
    private LocalDateTime createdDate;

    // 상품 정보
    private Long productId;
    private String productTitle;
    private Integer productPrice;
    private String productImage;

    // 무통장입금 정보
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    // 거래 당사자
    private String buyerName;
    private String sellerName;

    public static TransactionDetailDto fromEntity(Transaction t) {
        var p = t.getProduct();

        return TransactionDetailDto.builder()
                .transactionId(t.getTransactionId())
                .status(t.getStatus().name())
                .depositorName(t.getDepositorName())
                .transactionDate(t.getTransactionDate())
                .createdDate(t.getCreatedDate())

                .productId(p.getProductId())
                .productTitle(p.getTitle())
                .productPrice(p.getPrice())
                .productImage(p.getMainImage())

                .bankName(p.getBankName())
                .accountNumber(p.getAccountNumber())
                .accountHolder(p.getAccountHolder())

                .buyerName(t.getBuyer().getNickname())
                .sellerName(t.getSeller().getNickname())
                .build();
    }
}
