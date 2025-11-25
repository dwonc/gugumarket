package com.project.gugumarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 카카오페이 결제 준비 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KakaoPayReadyResponse {
    private String tid;                          // 결제 고유번호

    @JsonProperty("next_redirect_pc_url")
    private String nextRedirectPcUrl;            // PC 웹 결제 URL

    @JsonProperty("next_redirect_mobile_url")
    private String nextRedirectMobileUrl;        // 모바일 웹 결제 URL

    @JsonProperty("next_redirect_app_url")
    private String nextRedirectAppUrl;           // 앱 결제 URL

    @JsonProperty("android_app_scheme")
    private String androidAppScheme;             // 안드로이드 앱 스킴

    @JsonProperty("ios_app_scheme")
    private String iosAppScheme;                 // iOS 앱 스킴

    @JsonProperty("created_at")
    private LocalDateTime createdAt;             // 결제 준비 요청 시각
}