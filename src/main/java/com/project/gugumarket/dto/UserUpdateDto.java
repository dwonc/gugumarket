package com.project.gugumarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 프로필 수정 전용 DTO
 *
 * 경로: src/main/java/com/project/gugumarket/dto/UserUpdateDto.java
 * 용도: 사용자가 마이페이지에서 프로필 정보를 수정할 때 사용
 *
 * UserDto와의 차이점:
 * - userName(아이디), password(비밀번호) 필드 없음
 * - 회원가입이 아닌 정보 수정용이므로 필수 검증을 최소화
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {

    /**
     * 닉네임
     * - 필수 입력 항목
     * - 다른 사용자에게 표시되는 이름
     */
    @NotEmpty(message = "닉네임은 필수 항목입니다.")
    private String nickname;

    /**
     * 이메일
     * - 필수 입력 항목
     * - 올바른 이메일 형식이어야 함
     */
    @NotEmpty(message = "이메일은 필수 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    /**
     * 전화번호
     * - 선택 입력 항목
     * - 010-1234-5678 형식 권장
     */
    private String phone;

    /**
     * 우편번호
     * - 필수 입력 항목
     * - 다음 주소 API에서 자동 입력됨
     */
    @NotEmpty(message = "우편번호는 필수 항목입니다.")
    private String postalCode;

    /**
     * 주소 (기본 주소)
     * - 필수 입력 항목
     * - 다음 주소 API에서 자동 입력됨
     */
    @NotEmpty(message = "주소는 필수 항목입니다.")
    private String address;

    /**
     * 상세 주소
     * - 필수 입력 항목
     * - 사용자가 직접 입력하는 건물명, 동/호수 등
     */
    @NotEmpty(message = "상세주소는 필수 항목입니다.")
    private String addressDetail;

    /**
     * 프로필 이미지 URL
     * - 선택 항목
     * - 파일 업로드 후 저장된 경로
     */
}