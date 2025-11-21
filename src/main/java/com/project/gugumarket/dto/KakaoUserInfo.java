package com.project.gugumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfo {
    private Long id;
    private String email;
    private String nickname;
    private String profileImage;
}