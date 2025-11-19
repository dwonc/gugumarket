package com.project.gugumarket.dto;

import com.project.gugumarket.entity.QnaAnswer;
import com.project.gugumarket.entity.QnaPost;
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
public class QnaSimpleDto {
    private Long qnaId;
    private String title;
    private String content;
    private Boolean isAnswered;
    private LocalDateTime createdDate;

    // ✅ QnaAnswer 정보 (간단하게)
    private List<QnaAnswerSimpleDto> answers;

    // ✅ user 제외! (순환 참조 방지)

    public static QnaSimpleDto fromEntity(QnaPost qnaPost) {
        // QnaAnswer → DTO 변환
        List<QnaAnswerSimpleDto> answerDtos = null;
        if (qnaPost.getQnaAnswers() != null) {
            answerDtos = qnaPost.getQnaAnswers().stream()
                    .map(QnaAnswerSimpleDto::fromEntity)
                    .collect(Collectors.toList());
        }

        return QnaSimpleDto.builder()
                .qnaId(qnaPost.getQnaId())
                .title(qnaPost.getTitle())
                .content(qnaPost.getContent())
                .isAnswered(qnaPost.getIsAnswered())
                .createdDate(qnaPost.getCreatedDate())
                .answers(answerDtos)
                .build();
    }
}