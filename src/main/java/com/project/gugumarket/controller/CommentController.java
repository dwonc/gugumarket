// src/main/java/com/project/gugumarket/controller/CommentController.java
package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CommentDto;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.CommentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 댓글 기능을 처리하는 REST API 컨트롤러
 * - 댓글 목록 조회
 * - 댓글 작성 (일반 댓글 및 대댓글)
 * - 댓글 수정
 * - 댓글 삭제
 *
 * 모든 응답은 JSON 형식으로 반환
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;
    private final EntityManager em;

    /**
     * 현재 로그인한 사용자 정보 조회
     *
     * @param auth Spring Security 인증 정보
     * @return 현재 로그인한 User 엔티티, 로그인하지 않았으면 null
     */
    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        String username = auth.getName();
        try {
            return em.createQuery("select u from User u where u.userName = :name", User.class)
                    .setParameter("name", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 특정 상품의 댓글 목록 조회
     * - 해당 상품에 작성된 모든 댓글 반환
     * - 현재 사용자가 작성한 댓글인지 여부 포함
     * - 댓글 총 개수 포함
     *
     * @param productId 댓글을 조회할 상품 ID
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comments: [...], count: n }
     */
    @GetMapping("/products/{id}/comments")
    public ResponseEntity<?> list(@PathVariable("id") Long productId, Authentication auth) {
        Product product = em.getReference(Product.class, productId);
        User me = currentUser(auth);
        Long currentUserId = (me == null) ? null : me.getUserId();
        List<CommentDto> list = commentService.list(product, currentUserId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("comments", list);
        body.put("count", list.size());
        return ResponseEntity.ok(body);
    }

    /**
     * 댓글 작성
     * - 일반 댓글: parentId 없이 작성
     * - 대댓글: parentId를 포함하여 작성
     * - 로그인한 사용자만 작성 가능
     * - 빈 내용은 작성 불가
     *
     * @param productId 댓글을 작성할 상품 ID
     * @param payload 요청 본문 { content: "댓글 내용", parentId: "부모댓글ID(선택)" }
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comment: {...}, count: n }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     *         - 내용 없음: 400 상태코드, { success: false, message: "..." }
     */
    @PostMapping("/products/{id}/comments")
    public ResponseEntity<?> create(@PathVariable("id") Long productId,
                                    @RequestBody Map<String, String> payload,
                                    Authentication auth) {
        // 로그인 여부 확인
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }

        // 댓글 내용 검증
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }

        // 대댓글인 경우 부모 댓글 ID 파싱
        Long parentId = null;
        try {
            String pid = payload.get("parentId");
            if (pid != null && !pid.isBlank()) parentId = Long.valueOf(pid);
        } catch (NumberFormatException ignore) {}

        // 댓글 저장 및 총 개수 조회
        Product product = em.getReference(Product.class, productId);
        CommentDto saved = commentService.create(product, me, content, parentId);
        long count = commentService.countByProductId(productId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "comment", saved,
                "count", count
        ));
    }

    /**
     * 댓글 수정
     * - 본인이 작성한 댓글만 수정 가능
     * - 로그인한 사용자만 수정 가능
     * - 빈 내용으로 수정 불가
     *
     * @param commentId 수정할 댓글 ID
     * @param payload 요청 본문 { content: "수정할 내용" }
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comment: {...} }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     *         - 내용 없음: 400 상태코드, { success: false, message: "..." }
     */
    @PutMapping("/comments/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long commentId,
                                    @RequestBody Map<String, String> payload,
                                    Authentication auth) {
        // 로그인 여부 확인
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }

        // 댓글 내용 검증
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }

        // 댓글 수정 (서비스에서 권한 검증)
        CommentDto dto = commentService.update(commentId, me, content);
        return ResponseEntity.ok(Map.of("success", true, "comment", dto));
    }

    /**
     * 댓글 삭제
     * - 본인이 작성한 댓글만 삭제 가능
     * - 로그인한 사용자만 삭제 가능
     * - 삭제 후 남은 댓글 총 개수 반환
     *
     * @param commentId 삭제할 댓글 ID
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, count: n }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     */
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long commentId, Authentication auth) {
        // 로그인 여부 확인
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }

        // 댓글 삭제 및 남은 개수 반환 (서비스에서 권한 검증)
        long count = commentService.delete(commentId, me);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }
}
