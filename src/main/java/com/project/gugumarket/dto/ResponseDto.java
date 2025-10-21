package com.project.gugumarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {
    private boolean success;
    private String message;
    private T data;

    // 성공 응답 (데이터 있음)
    public static <T> ResponseDto<T> success(String message, T data) {
        return new ResponseDto<>(true, message, data);
    }

    // 성공 응답 (데이터 없음)
    public static <T> ResponseDto<T> success(String message) {
        return new ResponseDto<>(true, message, null);
    }

    // 실패 응답
    public static <T> ResponseDto<T> fail(String message) {
        return new ResponseDto<>(false, message, null);
    }
}