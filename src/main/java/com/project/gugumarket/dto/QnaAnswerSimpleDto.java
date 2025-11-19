package com.project.gugumarket.dto;

import com.project.gugumarket.entity.QnaAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QnaAnswerSimpleDto {
    private Long answerId;
    private String content;
    private String adminName;  // ✅ admin.getUserName()
    private LocalDateTime createdDate;

    // ✅ qnaPost, admin 엔티티 제외! (순환 참조 방지)

    public static QnaAnswerSimpleDto fromEntity(QnaAnswer qnaAnswer) {
        return QnaAnswerSimpleDto.builder()
                .answerId(qnaAnswer.getAnswerId())
                .content(qnaAnswer.getContent())
                .adminName(qnaAnswer.getAdmin() != null ? qnaAnswer.getAdmin().getUserName() : null)
                .createdDate(qnaAnswer.getCreatedDate())
                .build();
    }
}