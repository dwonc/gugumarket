package com.project.gugumarket.dto;

import com.project.gugumarket.entity.Comment;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CommentDto {
    private Long id;
    private Long userId;
    private String userNickname;
    private String content;
    private String createdAt;   // yyyy-MM-dd HH:mm
    private boolean mine;       // 현재 로그인 사용자의 댓글 여부

    public static CommentDto from(Comment c, Long currentUserId) {
        return CommentDto.builder()
                .id(c.getCommentId())
                .userId(c.getUser().getUserId())
                .userNickname(c.getUser().getNickname())
                .content(c.getContent())
                .createdAt(c.getCreatedDate() == null ? "" :
                        c.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .mine(currentUserId != null && currentUserId.equals(c.getUser().getUserId()))
                .build();
    }
}
