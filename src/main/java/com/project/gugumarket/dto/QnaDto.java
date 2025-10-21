package com.project.gugumarket.dto;

import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QnaDto {
    private Long qnaId;
    private Long userId;           // ← User 객체 대신
    private String nickName;       // ← 사용자 이름
    private String title;
    private String content;
    private Boolean isAnswered;
    private LocalDateTime createdDate;

    public static QnaDto fromEntity(QnaPost qnaPost) {
        return QnaDto.builder()
                .qnaId(qnaPost.getQnaId())  // ← 1번만!
                .userId(qnaPost.getUser() != null ? qnaPost.getUser().getUserId() : null)
                .nickName(qnaPost.getUser() != null ? qnaPost.getUser().getNickname(): "알 수 없음")
                .title(qnaPost.getTitle())
                .content(qnaPost.getContent())
                .isAnswered(qnaPost.getIsAnswered())
                .createdDate(qnaPost.getCreatedDate())
                .build();
    }
}

