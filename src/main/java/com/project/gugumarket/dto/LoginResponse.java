package com.project.gugumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String username;
    private String email;
    private String role;

    // ✅ 신규 추가: 주소 입력 필요 여부
    private Boolean requiresAddressUpdate;

    // ✅ 신규 추가: 사용자 정보 (모달에서 사용)
    private UserResponseDto user;
}