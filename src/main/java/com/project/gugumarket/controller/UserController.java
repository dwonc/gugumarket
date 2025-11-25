package com.project.gugumarket.controller;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;  // 🔥 이거 import 필요!
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    private final UserService userService;

    /**
     * ✅ 아이디 중복 체크
     * GET /api/users/check-username?username=test123
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @RequestParam String username
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📥 아이디 중복 체크 - 아이디: {}", username);

            boolean isDuplicate = userService.isUserNameDuplicate(username);

            response.put("success", true);
            response.put("isDuplicate", isDuplicate);
            response.put("message", isDuplicate ?
                    "이미 사용 중인 아이디입니다." :
                    "사용 가능한 아이디입니다.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 아이디 중복 체크 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "아이디 중복 확인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ 회원가입
     * POST /api/users/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody UserDto userDto,
            BindingResult bindingResult
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📥 회원가입 요청 - 아이디: {}", userDto.getUserName());

            // 1. Validation 에러 체크
            if (bindingResult.hasErrors()) {
                log.warn("⚠️ 유효성 검증 실패");
                response.put("success", false);
                response.put("message", "입력된 정보를 확인해주세요.");

                // Field-level 에러 반환
                Map<String, String> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.put(error.getField(), error.getDefaultMessage());
                }
                response.put("errors", errors);

                return ResponseEntity.badRequest().body(response);
            }

            // 2. 비밀번호 확인 검증
            if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
                log.warn("⚠️ 비밀번호 불일치");
                response.put("success", false);
                response.put("field", "passwordConfirm");
                response.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 회원가입 처리
            User createdUser = userService.create(userDto);

            log.info("✅ 회원가입 성공 - userId: {}", createdUser.getUserId());

            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다!");
            response.put("userId", createdUser.getUserId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // UserService에서 던진 예외 처리
            log.error("❌ 회원가입 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("❌ 회원가입 오류: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "회원가입 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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

    // 🆕🆕🆕 회원 등급 관련 API 추가 🆕🆕🆕

    /**
     * 🥚 내 등급 정보 조회
     * GET /api/users/me/level
     */
    @GetMapping("/me/level")
    public ResponseEntity<?> getMyLevel(Principal principal) {  // 🔥 Principal 추가!
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(principal.getName());
            UserLevelDto levelInfo = UserLevelDto.from(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "levelInfo", levelInfo
            ));
        } catch (Exception e) {
            log.error("❌ 등급 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "등급 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 🥚 특정 사용자 등급 조회
     * GET /api/users/{userId}/level
     */
    @GetMapping("/{userId}/level")
    public ResponseEntity<?> getUserLevel(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            UserLevelDto levelInfo = UserLevelDto.from(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "levelInfo", levelInfo
            ));
        } catch (Exception e) {
            log.error("❌ 사용자 등급 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "등급 조회 중 오류가 발생했습니다."));
        }
    }
}