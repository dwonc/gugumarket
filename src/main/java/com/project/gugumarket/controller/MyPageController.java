// src/main/java/com/project/gugumarket/controller/MyPageController.java
package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductDto;
import com.project.gugumarket.entity.Like;
import com.project.gugumarket.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
public class MyPageController {

    private final LikeService likeService;

    @GetMapping("/mypage")
    public String listList(Principal principal, Model model) {
        if (principal == null) {
            model.addAttribute("error", "로그인이 필요합니다.");
            return "mypage"; // 에러 메시지만 표시
        }

        Page<Like> likePage = likeService.myLikesByPrincipal(principal.getName(), PageRequest.of(0, 12));

        var productDtos = likePage.map(like ->
                ProductDto.of(
                        ProductDto.extractId(like.getProduct()),
                        likeService.countLikes(ProductDto.extractId(like.getProduct()))
                )
        ).getContent();

        model.addAttribute("likes", productDtos);
        model.addAttribute("page", likePage);
        return "mypage";
    }
}
