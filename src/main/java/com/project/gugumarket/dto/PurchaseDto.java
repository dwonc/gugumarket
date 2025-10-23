package com.project.gugumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseDto {
    private String depositorName;  // 입금자명
    private String phone;          // 구매자 연락처
    private String address;        // 배송지
    private String message;        // 판매자에게 메시지
}