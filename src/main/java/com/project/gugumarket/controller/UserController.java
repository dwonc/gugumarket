package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public ResponseEntity<Map<String, Object>> signupForm() {
        Map<String, Object> response = new HashMap<>();
        response.put("userDto", new UserDto());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody UserDto userDto, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        //유효성 검증 실패 시
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : ""
                    ));
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        //비밀번호 일치 확인
        if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            response.put("success", false);
            response.put("field", "passwordConfirm");
            response.put("message", "2개의 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            System.out.println("DB 저장 시도...");
            userService.create(userDto);
            System.out.println("컨트롤러 - 회원가입 성공");

            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다!");
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "이미 등록된 사용자입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> loginForm(@RequestParam(value = "error", required = false) String error) {
        Map<String, Object> response = new HashMap<>();

        if (error != null) {
            response.put("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return ResponseEntity.ok(response);
    }

    // 아이디 중복 체크
    @PostMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestBody Map<String, String> request) {
        String userName = request.get("userName");
        Map<String, Object> response = new HashMap<>();

        System.out.println("아이디 중복 체크: " + userName);

        boolean isDuplicate = userService.isUserNameDuplicate(userName);

        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");

        System.out.println("중복 체크 결과: " + (isDuplicate ? "중복" : "사용가능"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        // 현재 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            // Spring Security의 로그아웃 핸들러 사용
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            System.out.println("로그아웃 성공 - 사용자: " + authentication.getName());

            result.put("success", true);
            result.put("message", "로그아웃되었습니다.");
        } else {
            result.put("success", false);
            result.put("message", "인증 정보가 없습니다.");
        }

        return ResponseEntity.ok(result);
    }
}