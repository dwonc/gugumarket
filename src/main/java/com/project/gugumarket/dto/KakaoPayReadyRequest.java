package com.project.gugumarket.dto;

import lombok.*;

/**
 * 카카오페이 결제 준비 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPayReadyRequest {
    private String cid;              // 가맹점 코드
    private String partnerOrderId;   // 가맹점 주문번호 (transactionId)
    private String partnerUserId;    // 가맹점 회원 id (userId)
    private String itemName;         // 상품명
    private Integer quantity;        // 상품 수량
    private Integer totalAmount;     // 총 금액
    private Integer taxFreeAmount;   // 비과세 금액
    private String approvalUrl;      // 결제 성공 시 redirect url
    private String cancelUrl;        // 결제 취소 시 redirect url
    private String failUrl;          // 결제 실패 시 redirect url
}