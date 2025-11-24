// src/main/java/com/project/gugumarket/service/CommentService.java
package com.project.gugumarket.service;

import com.project.gugumarket.dto.CommentDto;
import com.project.gugumarket.entity.Comment;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommentDto> list(Product product, Long currentUserId) {
        return commentRepository
                .findByProduct_ProductIdAndIsDeletedFalseOrderByCreatedDateAsc(product.getProductId())
                .stream()
                .map(c -> CommentDto.from(c, currentUserId))
                .toList();
    }
    
    /** ✅ 댓글 작성 + 알림 전송 */
    public CommentDto create(Product product, User user, String content, Long parentId) {
        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));
            // 같은 상품의 댓글인지 안전 체크
            if (!parent.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalStateException("부모 댓글과 상품이 다릅니다.");
            }
        }

        Comment c = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .isDeleted(false)
                .parent(parent)
                .build();
        
        Comment saved = commentRepository.save(c);
        log.info("✅ 댓글 작성 완료: commentId={}, writer={}", saved.getCommentId(), user.getNickname());

        // ✅ 알림 전송
        sendCommentNotification(saved, product, parent);

        return CommentDto.from(saved, user.getUserId());
    }

    public CommentDto create(Product product, User user, String content) {
        return create(product, user, content, null);
    }

    /**
     * ✅ 댓글 알림 전송
     */
    private void sendCommentNotification(Comment comment, Product product, Comment parent) {
        try {
            User commenter = comment.getUser();  // 댓글 작성자
            
            // 1️⃣ 대댓글인 경우: 원댓글 작성자에게 알림
            if (parent != null) {
                User parentWriter = parent.getUser();
                
                // 본인이 본인 댓글에 답글 단 경우는 제외
                if (!parentWriter.getUserId().equals(commenter.getUserId())) {
                    String message = String.format(
                        "%s님이 회원님의 댓글에 답글을 남겼습니다: \"%s\"",
                        commenter.getNickname(),
                        truncate(comment.getContent(), 30)
                    );
                    
                    notificationService.createCommentNotification(
                        parentWriter,      // receiver
                        commenter,         // sender
                        product,           // product
                        comment.getContent()  // comment
                    );
                    
                    log.info("📧 대댓글 알림 전송: {} → {}", 
                        commenter.getNickname(), parentWriter.getNickname());
                }
            }
            // 2️⃣ 일반 댓글인 경우: 상품 판매자에게 알림
            else {
                User seller = product.getSeller();
                
                // 판매자가 본인 상품에 댓글 단 경우는 제외
                if (!seller.getUserId().equals(commenter.getUserId())) {
                    String message = String.format(
                        "%s님이 회원님의 상품 \"%s\"에 댓글을 남겼습니다: \"%s\"",
                        commenter.getNickname(),
                        truncate(product.getTitle(), 20),
                        truncate(comment.getContent(), 30)
                    );
                    
                    notificationService.createCommentNotification(
                        seller,            // receiver
                        commenter,         // sender
                        product,           // product
                        comment.getContent()  // comment
                    );
                    
                    log.info("📧 댓글 알림 전송: {} → {} (상품: {})", 
                        commenter.getNickname(), seller.getNickname(), product.getTitle());
                }
            }
        } catch (Exception e) {
            log.error("❌ 댓글 알림 전송 실패: {}", e.getMessage(), e);
            // 알림 실패해도 댓글은 저장되도록 예외를 먹음
        }
    }

    /**
     * ✅ 텍스트 자르기 헬퍼
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    public CommentDto update(Long commentId, User user, String content) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!c.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인 댓글만 수정할 수 있습니다.");
        }
        c.setContent(content);
        
        log.info("✅ 댓글 수정 완료: commentId={}", commentId);
        
        return CommentDto.from(c, user.getUserId());
    }

    public long delete(Long commentId, User user) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!c.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인 댓글만 삭제할 수 있습니다.");
        }
        
        Product product = c.getProduct();
        commentRepository.delete(c);
        
        log.info("✅ 댓글 삭제 완료: commentId={}", commentId);
        
        return commentRepository.countByProduct_ProductIdAndIsDeletedFalse(product.getProductId());
    }

    @Transactional(readOnly = true)
    public long countByProductId(Long productId) {
        return commentRepository.countByProduct_ProductIdAndIsDeletedFalse(productId);
    }
}
