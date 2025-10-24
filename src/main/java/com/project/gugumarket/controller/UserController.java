package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 관련 요청을 처리하는 컨트롤러
 * 회원가입, 로그인, 로그아웃, 아이디 중복 체크 등의 기능을 담당
 */
@RequiredArgsConstructor  // final 필드에 대한 생성자를 자동으로 생성 (의존성 주입)
@Controller  // Spring MVC 컨트롤러로 등록
@RequestMapping("/users")  // 모든 요청 경로 앞에 /users 접두사 추가
public class UserController {

    private final UserService userService;  // 사용자 비즈니스 로직을 처리하는 서비스

    /**
     * 회원가입 폼 페이지를 보여주는 메서드
     * GET /users/signup 요청 처리
     */
    @GetMapping("/signup")
    public String signupForm(Model model) {
        // 빈 UserDto 객체를 모델에 추가하여 폼에서 사용할 수 있도록 함
        model.addAttribute("userDto", new UserDto());
        return "users/signup";  // users/signup.html 템플릿 반환
    }

    /**
     * 회원가입 처리 메서드
     * POST /users/signup 요청 처리
     */
    @PostMapping("/signup")
    public String create(@Valid UserDto userDto,  // @Valid: 유효성 검증 수행
                         BindingResult bindingResult,  // 유효성 검증 결과를 담는 객체
                         Model model) {

        // 유효성 검증 실패 시 (예: 필수 필드 누락, 형식 오류 등)
        if (bindingResult.hasErrors()) {
            return "users/signup";  // 다시 회원가입 페이지로 이동
        }

        // 비밀번호와 비밀번호 확인 필드가 일치하는지 검증
        if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            // 일치하지 않으면 에러 메시지 추가
            bindingResult.rejectValue("passwordconfirm", "passwordInCorrect",
                    "2개의 비밀번호가 일치하지 않습니다.");
            return "users/signup";  // 회원가입 페이지로 돌아감
        }

        try {
            System.out.println("DB 저장 시도...");
            // 사용자 정보를 데이터베이스에 저장
            userService.create(userDto);
            model.addAttribute("successMessage", "회원가입이 완료되었습니다!");
            System.out.println("컨트롤러 - 회원가입 성공");
            // 회원가입 성공 시 로그인 페이지로 리다이렉트 (성공 메시지 파라미터 포함)
            return "redirect:/users/login?signup=success";
        } catch (DataIntegrityViolationException e) {
            // 데이터 무결성 위반 예외 (예: 중복된 아이디/이메일)
            e.printStackTrace();
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "users/signup";
        } catch (Exception e) {
            // 그 외 모든 예외 처리
            e.printStackTrace();
            bindingResult.reject("signupFailed", e.getMessage());
            return "users/signup";
        }
    }

    /**
     * 로그인 폼 페이지를 보여주는 메서드
     * GET /users/login 요청 처리
     */
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,  // URL 파라미터로 에러 여부 확인
                            Model model) {
        // 로그인 실패 시 에러 메시지 표시
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "users/login";  // users/login.html 템플릿 반환
    }

    /**
     * 아이디 중복 체크를 위한 AJAX 요청 처리 메서드
     * POST /users/check-username 요청 처리
     */
    @PostMapping("/check-username")
    @ResponseBody  // JSON 형태로 응답을 반환
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestBody Map<String, String> request) {
        // 요청 본문에서 userName 추출
        String userName = request.get("userName");
        Map<String, Object> response = new HashMap<>();

        System.out.println("아이디 중복 체크: " + userName);

        // 서비스를 통해 아이디 중복 여부 확인
        boolean isDuplicate = userService.isUserNameDuplicate(userName);

        // 응답 데이터 구성
        response.put("isDuplicate", isDuplicate);
        response.put("message", isDuplicate ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");

        System.out.println("중복 체크 결과: " + (isDuplicate ? "중복" : "사용가능"));

        // JSON 형태로 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃 처리 메서드
     * POST /users/logout 요청 처리
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // SecurityContext에서 현재 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            // Spring Security의 로그아웃 핸들러를 사용하여 로그아웃 처리
            // - 세션 무효화
            // - SecurityContext 클리어
            // - 쿠키 삭제 등
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            System.out.println("로그아웃 성공 - 사용자: " + authentication.getName());
        }

        // 메인 페이지로 리다이렉트
        return "redirect:/";
    }
}