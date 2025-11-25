package com.project.gugumarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 카카오페이 결제 승인 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KakaoPayApproveResponse {
    private String aid;                          // 요청 고유 번호
    private String tid;                          // 결제 고유 번호
    private String cid;                          // 가맹점 코드
    private String sid;                          // 정기결제용 ID (정기결제 시)

    @JsonProperty("partner_order_id")
    private String partnerOrderId;               // 가맹점 주문번호

    @JsonProperty("partner_user_id")
    private String partnerUserId;                // 가맹점 회원 id

    @JsonProperty("payment_method_type")
    private String paymentMethodType;            // 결제 수단 (CARD, MONEY 등)

    private Amount amount;                       // 결제 금액 정보

    @JsonProperty("card_info")
    private CardInfo cardInfo;                   // 카드 정보

    @JsonProperty("item_name")
    private String itemName;                     // 상품 이름

    @JsonProperty("item_code")
    private String itemCode;                     // 상품 코드

    private Integer quantity;                    // 상품 수량

    @JsonProperty("created_at")
    private LocalDateTime createdAt;             // 결제 준비 요청 시각

    @JsonProperty("approved_at")
    private LocalDateTime approvedAt;            // 결제 승인 시각

    private String payload;                      // 결제 승인 요청에 대해 저장한 값

    /**
     * 결제 금액 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Amount {
        private Integer total;                   // 전체 결제 금액

        @JsonProperty("tax_free")
        private Integer taxFree;                 // 비과세 금액

        private Integer vat;                     // 부가세 금액
        private Integer point;                   // 사용한 포인트 금액
        private Integer discount;                // 할인 금액

        @JsonProperty("green_deposit")
        private Integer greenDeposit;            // 컵 보증금
    }

    /**
     * 카드 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CardInfo {
        @JsonProperty("kakaopay_purchase_corp")
        private String kakaopayPurchaseCorp;     // 카카오페이 매입사명

        @JsonProperty("kakaopay_purchase_corp_code")
        private String kakaopayPurchaseCorpCode; // 카카오페이 매입사 코드

        @JsonProperty("kakaopay_issuer_corp")
        private String kakaopayIssuerCorp;       // 카카오페이 발급사명

        @JsonProperty("kakaopay_issuer_corp_code")
        private String kakaopayIssuerCorpCode;   // 카카오페이 발급사 코드

        @JsonProperty("bin")
        private String bin;                      // 카드 BIN

        @JsonProperty("card_type")
        private String cardType;                 // 카드 타입

        @JsonProperty("install_month")
        private String installMonth;             // 할부 개월 수

        @JsonProperty("approved_id")
        private String approvedId;               // 카드사 승인번호

        @JsonProperty("card_mid")
        private String cardMid;                  // 카드사 가맹점 번호

        @JsonProperty("interest_free_install")
        private String interestFreeInstall;      // 무이자할부 여부(Y/N)

        @JsonProperty("installment_type")
        private String installmentType;          // 할부 유형

        @JsonProperty("card_item_code")
        private String cardItemCode;             // 카드 상품 코드
    }
}