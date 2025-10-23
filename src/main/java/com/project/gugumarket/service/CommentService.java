package com.project.gugumarket.service;

import com.project.gugumarket.dto.CommentDto;
import com.project.gugumarket.entity.Comment;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<CommentDto> list(Product product, Long currentUserId) {
        return commentRepository
                .findByProduct_ProductIdAndIsDeletedFalseOrderByCreatedDateAsc(product.getProductId())
                .stream()
                .map(c -> CommentDto.from(c, currentUserId))
                .toList();
    }

    public CommentDto create(Product product, User user, String content) {
        Comment c = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .isDeleted(false)
                .build();
        commentRepository.save(c);
        return CommentDto.from(c, user.getUserId());
    }

    public CommentDto update(Long commentId, User user, String content) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!c.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인 댓글만 수정할 수 있습니다.");
        }
        c.setContent(content);
        return CommentDto.from(c, user.getUserId());
    }

    /** ✅ 완전 삭제 */
    public long delete(Long commentId, User user) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!c.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(c);
        return commentRepository.countByProduct_ProductIdAndIsDeletedFalse(c.getProduct().getProductId());
    }

    @Transactional(readOnly = true)
    public long countByProductId(Long productId) {
        return commentRepository.countByProduct_ProductIdAndIsDeletedFalse(productId);
    }
}
