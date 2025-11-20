package com.project.gugumarket.dto;

import com.project.gugumarket.entity.QnaPost;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaResponseDto {

    private Long qnaId;
    private Long userId;
    private String nickName;
    private String title;
    private String content;
    private Boolean isAnswered;
    private LocalDateTime createdDate;

    /**
     * QnaPost Entity → DTO 변환
     */
    public static QnaResponseDto fromEntity(QnaPost qnaPost) {
        return QnaResponseDto.builder()
                .qnaId(qnaPost.getQnaId())
                .userId(qnaPost.getUser() != null ? qnaPost.getUser().getUserId() : null)
                .nickName(qnaPost.getUser() != null ? qnaPost.getUser().getNickname() : "알 수 없음")
                .title(qnaPost.getTitle())
                .content(qnaPost.getContent())
                .isAnswered(qnaPost.getIsAnswered())
                .createdDate(qnaPost.getCreatedDate())
                .build();
    }
}