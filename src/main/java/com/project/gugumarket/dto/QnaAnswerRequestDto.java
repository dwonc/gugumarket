package com.project.gugumarket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QnaAnswerRequestDto {
    private Long qnaId;

    @NotBlank(message = "답변 내용을 입력해주세요.")
    private String content;
}