package com.project.gugumarket.controller;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")  // ✅ 이 경로 확인
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    private final UserService userService;

    /**
     * 아이디 찾기
     * POST /api/users/find-username
     */
    @PostMapping("/find-username")
    public ResponseEntity<ResponseDto<FindUsernameResponse>> findUsername(
            @Valid @RequestBody FindUsernameRequest request
    ) {
        try {
            log.info("📥 아이디 찾기 요청 - 이메일: {}", request.getEmail());

            FindUsernameResponse response = userService.findUsername(request);

            return ResponseEntity.ok(
                    ResponseDto.success("아이디를 찾았습니다.", response)
            );

        } catch (Exception e) {
            log.error("❌ 아이디 찾기 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail(e.getMessage()));
        }
    }

    /**
     * 비밀번호 재설정을 위한 이메일 인증
     * POST /api/users/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ResponseDto<VerifyEmailResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        try {
            log.info("📥 이메일 인증 요청 - 아이디: {}, 이메일: {}",
                    request.getUserName(), request.getEmail());

            VerifyEmailResponse response = userService.verifyEmailForPasswordReset(request);

            return ResponseEntity.ok(
                    ResponseDto.success("이메일 인증 성공", response)
            );

        } catch (Exception e) {
            log.error("❌ 이메일 인증 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail(e.getMessage()));
        }
    }

    /**
     * 비밀번호 재설정
     * POST /api/users/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDto<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            log.info("📥 비밀번호 재설정 요청");

            userService.resetPassword(request);

            return ResponseEntity.ok(
                    ResponseDto.success("비밀번호가 성공적으로 변경되었습니다.", null)
            );

        } catch (Exception e) {
            log.error("❌ 비밀번호 재설정 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail(e.getMessage()));
        }
    }
}