package com.project.gugumarket.dto;

import com.project.gugumarket.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserResponseDto {
    private Long userId;
    private String userName;
    private String email;
    private String nickname;
    private String phone;
    private String address;
    private String addressDetail;
    private String postalCode;
    private String profileImage;
    private LocalDateTime createdDate;
    private Boolean isActive;
    private String role;

    // ✅ products는 포함하지 않음! (순환 참조 방지)

    
    // User 엔티티 → DTO 변환
    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .address(user.getAddress())
                .addressDetail(user.getAddressDetail())
                .postalCode(user.getPostalCode())
                .profileImage(user.getProfileImage())
                .createdDate(user.getCreatedDate())
                .isActive(user.getIsActive())
                .role(user.getRole())
                .build();
    }
}