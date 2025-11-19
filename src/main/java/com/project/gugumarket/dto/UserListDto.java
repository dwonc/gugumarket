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
public class UserListDto {
    private Long userId;
    private String userName;
    private String email;
    private String nickname;
    private String phone;
    private String profileImage;
    private LocalDateTime createdDate;
    private Boolean isActive;
    private String role;

    // ✅ products, likes 제외 (순환 참조 방지)

    public static UserListDto fromEntity(User user) {
        return UserListDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .profileImage(user.getProfileImage())
                .createdDate(user.getCreatedDate())
                .isActive(user.getIsActive())
                .role(user.getRole())
                .build();
    }
}