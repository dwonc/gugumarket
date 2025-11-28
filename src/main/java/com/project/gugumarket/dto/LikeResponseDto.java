package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 찜(좋아요) 정보를 클라이언트에게 전달하기 위한 DTO (Data Transfer Object)
 * Entity를 직접 노출하지 않고 필요한 데이터만 선택적으로 전송
 */
@Getter // 모든 필드에 대한 Getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용한 객체 생성 지원
@NoArgsConstructor // 인자(Arguments)가 없는 기본 생성자를 자동으로 생성
@AllArgsConstructor // 해당 클래스의 모든 필드를 인자로 받는 생성자를 자동으로 생성
public class LikeResponseDto {
    // 찜 고유 ID
    private Long likeId;
    // 찜한 상품의 ID
    private Long productId;
    // 찜한 상품의 제목
    private String productTitle;
    // 찜한 상품의 가격
    private Integer productPrice;
    // 찜한 상품의 대표 이미지 URL
    private String productImage;
    // 찜한 상품의 상태 (판매중, 예약중, 판매완료 등)
    private String productStatus;
    // 찜을 등록한 날짜 및 시간
    private LocalDateTime createdDate;

    /**
     * Like 엔티티를 LikeResponseDto로 변환하는 정적 팩토리 메서드
     *
     * @param like 변환할 Like 엔티티
     * @return 변환된 LikeResponseDto 객체
     */
    public static LikeResponseDto fromEntity(Like like) {
        // Like 엔티티에서 연관된 Product 엔티티 가져오기
        Product product = like.getProduct();
        // 상품의 메인 이미지 URL 가져오기
        String imageUrl = product.getMainImage(); // ✅ getFirstImageUrl() → getMainImage()로 변경

        // 메인 이미지가 없는 경우, 첫 번째 productImages 사용
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrl = product.getProductImages().getFirst().getImageUrl();
        }

        // 빌더 패턴을 사용하여 DTO 객체 생성 및 반환
        return LikeResponseDto.builder()
                .likeId(like.getLikeId()) // 찜 ID 설정
                .productId(product.getProductId()) // 상품 ID 설정
                .productTitle(product.getTitle()) // 상품 제목 설정
                .productPrice(product.getPrice()) // 상품 가격 설정
                .productImage(imageUrl)  // ✅ 상품 이미지 URL 설정
                .productStatus(product.getStatus().name()) // 상품 상태를 문자열로 변환하여 설정
                .createdDate(like.getCreatedDate()) // 찜 등록 날짜 설정
                .build(); // DTO 객체 생성
    }
}