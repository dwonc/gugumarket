package com.project.gugumarket.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter  // status 필드의 getter 메서드 자동 생성
@Setter  // status 필드의 setter 메서드 자동 생성
@NoArgsConstructor  // 기본 생성자 자동 생성
@AllArgsConstructor  // 모든 필드를 받는 생성자 자동 생성
public class ProductStatusRequest {

  @NotBlank(message = "상태를 입력해주세요.")
    private String status;
  
}
