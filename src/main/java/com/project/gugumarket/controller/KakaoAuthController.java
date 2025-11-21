package com.project.gugumarket.controller;

import com.project.gugumarket.dto.LoginResponse;
import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.service.KakaoAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true") // 🔥 추가
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;

    @GetMapping("/callback")
    public ResponseEntity<ResponseDto<LoginResponse>> kakaoCallback(
            @RequestParam("code") String code
    ) {
        System.out.println("🎯🎯🎯 카카오 콜백 도달! code: " + code); // 🔥 System.out 사용
        log.info("🎯🎯🎯 카카오 콜백 도달! code: {}", code);

        try {
            LoginResponse loginResponse = kakaoAuthService.kakaoLogin(code);

            return ResponseEntity.ok(
                    ResponseDto.success("카카오 로그인 성공", loginResponse)
            );

        } catch (Exception e) {
            System.err.println("❌ 에러: " + e.getMessage()); // 🔥 에러도 System.err
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("카카오 로그인 실패: " + e.getMessage()));
        }
    }

    // 🔥 테스트용 엔드포인트 추가
    @GetMapping("/test")
    public String test() {
        System.out.println("✅ 테스트 엔드포인트 도달!");
        return "OK";
    }
}