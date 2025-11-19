package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {
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

    // ✅ Entity 대신 DTO 사용!
    private List<ProductSimpleDto> products;
    private List<QnaSimpleDto> qnaPosts;
    private long productCount;
    private long qnaCount;

    public static UserDetailDto fromEntity(User user, List<Product> products, List<QnaPost> qnaPosts) {
        // ✅ Entity → DTO 변환
        List<ProductSimpleDto> productDtos = products.stream()
                .map(ProductSimpleDto::fromEntity)
                .collect(Collectors.toList());

        List<QnaSimpleDto> qnaDtos = qnaPosts.stream()
                .map(QnaSimpleDto::fromEntity)
                .collect(Collectors.toList());

        return UserDetailDto.builder()
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
                .products(productDtos)
                .qnaPosts(qnaDtos)
                .productCount(productDtos.size())
                .qnaCount(qnaDtos.size())
                .build();
    }
}