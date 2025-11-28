package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 거래 정보를 클라이언트에게 전달하기 위한 DTO (Data Transfer Object)
 * 구매 내역과 판매 내역 조회 시 사용
 */
@Getter // 모든 필드에 대한 Getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용한 객체 생성 지원
@NoArgsConstructor // 인자가 없는 기본 생성자를 자동으로 생성
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 자동으로 생성
public class TransactionResponseDto {
    // 거래 고유 ID
    private Long transactionId;
    // 거래된 상품의 ID
    private Long productId;
    // 거래된 상품의 제목
    private String productTitle;
    // 거래된 상품의 가격
    private Integer productPrice;
    // 거래된 상품의 대표 이미지 URL
    private String productImage;
    // 구매자 닉네임
    private String buyerName;
    // 구매자 ID
    private Long buyerId;
    // 판매자 닉네임
    private String sellerName;
    // 입금자명 (실제 송금한 사람의 이름)
    private String depositorName;  // ✅ 추가
    // 거래 상태 (대기중, 입금완료, 거래완료, 취소 등)
    private String status;
    // 거래가 완료된 날짜 및 시간
    private LocalDateTime transactionDate;
    // 거래 정보가 생성된 날짜 및 시간
    private LocalDateTime createdDate;  // ✅ 추가

    /**
     * Transaction 엔티티를 TransactionResponseDto로 변환하는 정적 팩토리 메서드
     *
     * @param transaction 변환할 Transaction 엔티티
     * @return 변환된 TransactionResponseDto 객체
     */
    public static TransactionResponseDto fromEntity(Transaction transaction) {
        // Transaction 엔티티에서 연관된 Product 엔티티 가져오기
        Product product = transaction.getProduct();
        // 상품의 메인 이미지 URL 가져오기
        String imageUrl = product.getMainImage(); // ✅ 수정

        // 메인 이미지가 없는 경우, 첫 번째 productImages 사용
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            imageUrl = product.getProductImages().getFirst().getImageUrl();
        }

        // 빌더 패턴을 사용하여 DTO 객체 생성 및 반환
        return TransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId()) // 거래 ID 설정
                .productId(transaction.getProduct().getProductId()) // 상품 ID 설정
                .productTitle(transaction.getProduct().getTitle()) // 상품 제목 설정
                .productPrice(transaction.getProduct().getPrice()) // 상품 가격 설정
                .productImage(transaction.getProduct().getMainImage()) // 상품 이미지 URL 설정
                .buyerName(transaction.getBuyer().getNickname()) // 구매자 닉네임 설정
                .sellerName(transaction.getSeller().getNickname()) // 판매자 닉네임 설정
                .depositorName(transaction.getDepositorName())  // ✅ 입금자명 설정
                .status(transaction.getStatus().name()) // 거래 상태를 문자열로 변환하여 설정
                .transactionDate(transaction.getTransactionDate()) // 거래 완료 날짜 설정
                .buyerId(transaction.getBuyer().getUserId()) // 구매자 ID 설정
                .createdDate(transaction.getCreatedDate())  // ✅ 거래 생성 날짜 설정
                .build(); // DTO 객체 생성
    }
}