package com.project.gugumarket.controller;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController                 //JSON만 반환
@RequiredArgsConstructor
@RequestMapping("/api")         //API 명시
public class LikeController {

    private final LikeService likeService;
    private final ProductService productService;
    private final UserService userService;

    /**
     *         좋아요 토글 (추가/삭제)
     */
    @PostMapping("/products/{productId}/like")  //RESTFULL 규칙으로 porduct (단수) -> products (복수) 로 변경
    public ResponseEntity<?> toggleLike(
            @PathVariable Long productId,       //URL의 productId 값을 변수로 받기
            Principal principal) {              // 현재 로그인한 사용자 정보 (로그인 안했으면 null)

        if (principal == null) {                // null =>로그인 안함
            return ResponseEntity.status(401)   // 인증이 필요하다는 에러창 401 페이지
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        /* 
        *       좋아요 처리 로직
        */
        try {
            User user = userService.getUser(principal.getName());       //현재 로그인한 사용자 조희 (사용자ID)
            Product product = productService.getProduct(productId);     //상품 정보 조회

            boolean isLiked = likeService.toggleLike(user, product);
            Long likeCount = likeService.getLikeCount(product);

            return ResponseEntity.ok(Map.of(            //성공 응답 200 페이지
                    "success", true,
                    "isLiked", isLiked,
                    "likeCount", likeCount,
                    "message", isLiked ? "찜했습니다!" : "찜을 취소했습니다."
            ));

        } catch (Exception e) {                         //예외 에러 처리
            return ResponseEntity.status(500)   //500 서버 내부 오류
                    .body(Map.of(
                            "success", false,
                            "message", "오류가 발생했습니다: " + e.getMessage()
                    ));
        }

    }

}