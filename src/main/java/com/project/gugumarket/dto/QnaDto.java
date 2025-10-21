package com.project.gugumarket.dto;

import com.project.gugumarket.entity.QnaPost;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaDto {

    private Long qnaId;
    private Long userId;
    private String nickName;  // User의 nickname 필드

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private Boolean isAnswered;
    private LocalDateTime createdDate;

    /**
     * Entity → DTO 변환
     */
    public static QnaDto fromEntity(QnaPost qnaPost) {
        return QnaDto.builder()
                .qnaId(qnaPost.getQnaId())
                .userId(qnaPost.getUser() != null ? qnaPost.getUser().getUserId() : null)
                .nickName(qnaPost.getUser() != null ? qnaPost.getUser().getNickname() : "알 수 없음")  // ✅ getNickname()
                .title(qnaPost.getTitle())
                .content(qnaPost.getContent())
                .isAnswered(qnaPost.getIsAnswered())
                .createdDate(qnaPost.getCreatedDate())
                .build();
    }

    /**
     * DTO → Entity 변환 (기본 정보만)
     */
    public static QnaPost toEntity(QnaDto qnaDto) {
        return QnaPost.builder()
                .title(qnaDto.getTitle())
                .content(qnaDto.getContent())
                .build();
    }
}