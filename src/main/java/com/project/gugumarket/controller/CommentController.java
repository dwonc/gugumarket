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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;
    private final EntityManager em;

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

    /** ✅ parentId(Optional) 받도록 확장 */
    @PostMapping("/products/{id}/comments")
    public ResponseEntity<?> create(@PathVariable("id") Long productId,
                                    @RequestBody Map<String, String> payload,
                                    Authentication auth) {
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }
        Long parentId = null;
        try {
            String pid = payload.get("parentId");
            if (pid != null && !pid.isBlank()) parentId = Long.valueOf(pid);
        } catch (NumberFormatException ignore) {}

        Product product = em.getReference(Product.class, productId);
        CommentDto saved = commentService.create(product, me, content, parentId);
        long count = commentService.countByProductId(productId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "comment", saved,
                "count", count
        ));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long commentId,
                                    @RequestBody Map<String, String> payload,
                                    Authentication auth) {
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }
        CommentDto dto = commentService.update(commentId, me, content);
        return ResponseEntity.ok(Map.of("success", true, "comment", dto));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long commentId, Authentication auth) {
        User me = currentUser(auth);
        if (me == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }
        long count = commentService.delete(commentId, me);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }
}
