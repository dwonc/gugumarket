package com.project.gugumarket.dto;

import lombok.*;

/**
 * 카카오페이 결제 승인 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoPayApproveRequest {
    private String cid;              // 가맹점 코드
    private String tid;              // 결제 고유번호
    private String partnerOrderId;   // 가맹점 주문번호
    private String partnerUserId;    // 가맹점 회원 id
    private String pgToken;          // 결제승인 요청 인증 토큰
}