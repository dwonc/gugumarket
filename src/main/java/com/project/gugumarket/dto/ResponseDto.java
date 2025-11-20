package com.project.gugumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 형식
 * 모든 REST API 응답을 일관된 형태로 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {

    private boolean success;        // 성공 여부
    private String message;         // 응답 메시지
    private T data;                 // 실제 데이터
    private String errorCode;       // 에러 코드 (실패 시)

    /**
     * 성공 응답 생성
     */
    public static <T> ResponseDto<T> success(String message, T data) {
        return ResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ResponseDto<T> success(String message) {
        return ResponseDto.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ResponseDto<T> fail(String message) {
        return ResponseDto.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * 실패 응답 생성 (에러 코드 포함)
     */
    public static <T> ResponseDto<T> fail(String message, String errorCode) {
        return ResponseDto.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
