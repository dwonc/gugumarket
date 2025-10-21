package com.project.gugumarket.controller;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final ProductService productService;
    private final UserService userService;

    /**
     * 좋아요 토글 (추가/삭제)
     */
    @PostMapping("/products/{productId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleLike(
            @PathVariable Long productId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            User user = userService.getUser(principal.getName());
            Product product = productService.getProduct(productId);

            boolean isLiked = likeService.toggleLike(user, product);
            Long likeCount = likeService.getLikeCount(product);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isLiked", isLiked,
                    "likeCount", likeCount,
                    "message", isLiked ? "찜했습니다!" : "찜을 취소했습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "success", false,
                            "message", "오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }
}