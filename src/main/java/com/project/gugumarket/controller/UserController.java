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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 REST API 컨트롤러
 * 회원가입, 아이디 찾기, 비밀번호 재설정, 등급 조회 등의 기능 제공
 */
@Slf4j  // 로깅 기능 활성화
@RestController  // REST API 컨트롤러임을 명시
@RequestMapping("/api/users")  // 기본 URL 경로
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성 (DI)
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")  // CORS 설정
public class UserController {

    // 사용자 비즈니스 로직을 처리하는 서비스
    private final UserService userService;

    /**
     * 아이디 중복 체크
     * 회원가입 시 사용자 아이디가 이미 존재하는지 확인
     *
     * @param username 중복 확인할 아이디
     * @return 중복 여부와 메시지를 포함한 응답
     *
     * GET /api/users/check-username?username=test123
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @RequestParam String username  // 쿼리 파라미터로 아이디 받기
    ) { //map=<key, value> object는 어떤 데이터도 담을 수 있다는 뜻 최상위 객체이기 때문
        //hashmap=map을 구현한 가장 일반적인 클래스
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📥 아이디 중복 체크 - 아이디: {}", username);

            // 서비스 계층에서 중복 여부 확인
            boolean isDuplicate = userService.isUserNameDuplicate(username);

            // 응답 데이터 구성
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
     * 회원가입
     * 새로운 사용자를 등록하는 API
     *
     * @param userDto 회원가입 정보 (아이디, 비밀번호, 이메일 등)
     * @param bindingResult 유효성 검증 결과
     * @return 회원가입 성공 여부와 메시지
     *
     * POST /api/users/signup
     *
     * 처리 순서:
     * 1. @Valid를 통한 필드 유효성 검증 (BindingResult)
     * 2. 비밀번호 확인 일치 여부 검증
     * 3. 서비스 계층에서 회원 생성
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody UserDto userDto,  // @Valid: DTO의 validation 어노테이션 검증 유효성,제약 조건 검증
            BindingResult bindingResult  // 유효성 검증 결과를 담는 객체 에러 메시지들을 저장
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📥 회원가입 요청 - 아이디: {}", userDto.getUserName());

            // 1. Validation 에러 체크
            // @NotBlank, @Email 등 DTO에 정의된 제약조건 검증 결과 확인
            if (bindingResult.hasErrors()) {
                log.warn("⚠️ 유효성 검증 실패");
                response.put("success", false);
                response.put("message", "입력된 정보를 확인해주세요.");

                // 필드별 에러 메시지 수집
                Map<String, String> errors = new HashMap<>();
                for (FieldError error : bindingResult.getFieldErrors()) {
                    errors.put(error.getField(), error.getDefaultMessage());
                }
                response.put("errors", errors);

                return ResponseEntity.badRequest().body(response);
            }

            // 2. 비밀번호 확인 검증
            // 사용자가 입력한 비밀번호와 비밀번호 확인이 일치하는지 확인
            if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
                log.warn("⚠️ 비밀번호 불일치");
                response.put("success", false);
                response.put("field", "passwordConfirm");
                response.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. 회원가입 처리
            // UserService에서 실제 회원 생성 로직 수행
            User createdUser = userService.create(userDto);

            log.info("✅ 회원가입 성공 - userId: {}", createdUser.getUserId());

            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다!");
            response.put("userId", createdUser.getUserId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // UserService에서 던진 비즈니스 로직 예외 처리
            // 예: 이미 존재하는 아이디, 이메일 등
            log.error("❌ 회원가입 실패: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            // 예상치 못한 서버 오류 처리
            log.error("❌ 회원가입 오류: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "회원가입 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 아이디 찾기
     * 사용자의 이메일을 통해 아이디를 찾는 기능
     *
     * @param request 이메일 정보를 포함한 요청
     * @return 찾은 아이디 정보
     *
     * POST /api/users/find-username
     *
     * 사용 시나리오: 사용자가 아이디를 잊어버렸을 때 이메일로 아이디 확인
     */
    @PostMapping("/find-username")
    public ResponseEntity<ResponseDto<FindUsernameResponse>> findUsername(
            @Valid @RequestBody FindUsernameRequest request
    ) {
        try {
            log.info("📥 아이디 찾기 요청 - 이메일: {}", request.getEmail());

            // 이메일로 사용자 아이디 조회
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
     * 비밀번호를 재설정하기 전에 아이디와 이메일이 일치하는지 확인
     *
     * @param request 아이디와 이메일 정보
     * @return 인증 성공 여부
     *
     * POST /api/users/verify-email
     *
     * 처리 흐름:
     * 1. 아이디와 이메일이 일치하는 사용자가 있는지 확인
     * 2. 일치하면 비밀번호 재설정 권한 부여
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ResponseDto<VerifyEmailResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        try {
            log.info("📥 이메일 인증 요청 - 아이디: {}, 이메일: {}",
                    request.getUserName(), request.getEmail());

            // 아이디와 이메일 일치 여부 확인
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
     * 이메일 인증 후 새로운 비밀번호로 변경
     *
     * @param request 새 비밀번호 정보
     * @return 재설정 성공 여부
     *
     * POST /api/users/reset-password
     *
     * 주의: 이 API는 반드시 이메일 인증(/verify-email) 후에 호출되어야 함
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDto<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            log.info("📥 비밀번호 재설정 요청");

            // 새 비밀번호로 변경
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

    // ========== 회원 등급 관련 API ==========

    /**
     * 내 등급 정보 조회
     * 현재 로그인한 사용자의 거래 등급 정보 반환
     *
     * @param principal Spring Security가 주입하는 인증 정보 (현재 로그인 사용자)
     * @return 등급 정보 (레벨, 거래 횟수, 다음 등급까지 필요한 거래 수 등)
     *
     * GET /api/users/me/level
     *
     * 등급 시스템:
     * - 🥚 알 (0-2회)
     * - 🐣 아기새 (3-9회)
     * - 🐥 사춘기새 (10-29회)
     * - 🦅 어른새 (30회+)
     */
    @GetMapping("/me/level")
    public ResponseEntity<?> getMyLevel(Principal principal) {
        // Principal: Spring Security가 인증된 사용자 정보를 자동으로 주입
        // principal.getName()으로 사용자 아이디(username) 조회 가능

        // 로그인하지 않은 경우 401 Unauthorized 반환
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // principal.getName()으로 현재 로그인 사용자의 아이디 가져오기
            User user = userService.getUser(principal.getName());

            // User 엔티티를 UserLevelDto로 변환 (등급 정보 포함)
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
     * 특정 사용자 등급 조회
     * 다른 사용자의 등급 정보를 조회할 때 사용 (공개 정보)
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자의 등급 정보
     *
     * GET /api/users/{userId}/level
     *
     * 사용 시나리오: 상품 상세 페이지에서 판매자의 등급을 표시할 때
     */
    @GetMapping("/{userId}/level")
    public ResponseEntity<?> getUserLevel(@PathVariable Long userId) {
        try {
            // userId로 사용자 조회
            User user = userService.getUserById(userId);

            // 등급 정보 DTO로 변환
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