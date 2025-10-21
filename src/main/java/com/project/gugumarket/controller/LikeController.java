package com.project.gugumarket.controller;

import com.project.gugumarket.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{productId}")
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable Long productId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("로그인이 필요합니다.");
        var result = likeService.toggleByPrincipal(productId, principal.getName());
        return ResponseEntity.ok(result); // { liked: true/false, likeCount: n }
    }
}
