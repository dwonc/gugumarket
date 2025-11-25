package com.project.gugumarket.dto;

import lombok.*;

/**
 * 구매 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDto {
    private Long productId;          // 상품 ID
    private String depositorName;    // 입금자명 (무통장 입금 시)
    private String phone;            // 구매자 전화번호
    private String address;          // 구매자 주소
    private String message;          // 판매자에게 메시지

    // 🔥 결제 수단 추가
    /**
     * 결제 수단
     * - BANK_TRANSFER: 무통장 입금
     * - KAKAOPAY: 카카오페이
     */
    private String paymentMethod;    // 결제 수단
}






















































































