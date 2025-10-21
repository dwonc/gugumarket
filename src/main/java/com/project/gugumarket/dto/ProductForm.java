package com.project.gugumarket.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductForm {

    private Long productId;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Long categoryId;

    @NotBlank(message = "상품명을 입력해주세요.")
    @Size(max = 100, message = "상품명은 100자 이내로 입력해주세요.")
    private String title;

    @NotNull(message = "가격을 입력해주세요.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotBlank(message = "상품 설명을 입력해주세요.")
    private String content;

    @NotBlank(message = "대표 이미지를 업로드해주세요.")
    private String mainImage;

    // 추가 이미지 URL 리스트
    private List<String> additionalImages;

    // 계좌 정보
    @NotBlank(message = "은행명을 선택해주세요.")
    private String bankName;

    @NotBlank(message = "계좌번호를 입력해주세요.")
    @Pattern(regexp = "^[0-9]+$", message = "계좌번호는 숫자만 입력 가능합니다.")
    private String accountNumber;

    @NotBlank(message = "예금주명을 입력해주세요.")
    private String accountHolder;
}