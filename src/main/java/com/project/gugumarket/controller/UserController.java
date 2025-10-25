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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "users/signup";
    }

    @PostMapping("/signup")
    public String create(@Valid UserDto userDto, BindingResult bindingResult, Model model) {

        //유효성 검증 실패 시
        if (bindingResult.hasErrors()) {
            return "users/signup";
        }

        //비밀번호 일치 확인
        if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordconfirm", "passwordInCorrect", "2개의 비밀번호가 일치하지 않습니다.");
            return "users/signup";
        }

        try {
            System.out.println("DB 저장 시도...");
            userService.create(userDto);
            model.addAttribute("successMessage", "회원가입이 완료되었습니다!");
            System.out.println("컨트롤러 - 회원가입 성공");
            return "redirect:/users/login?signup=success";
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "users/signup";
        } catch (Exception e) {
            e.printStackTrace();
            bindingResult.reject("signupFailed", e.getMessage());
            return "users/signup";
        }
    }
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return "users/login";
    }
    // 아이디 중복 체크
    @PostMapping("/check-username")
    @ResponseBody
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
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // 현재 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            // Spring Security의 로그아웃 핸들러 사용
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            System.out.println("로그아웃 성공 - 사용자: " + authentication.getName());
        }

        return "redirect:/";
    }

    /**
     * 아이디 찾기 페이지
     */
    @GetMapping("/find-id")
    public String findIdForm() {
        return "users/findid";
    }

    /**
     * 이메일로 아이디 찾기
     */

    @PostMapping("/find-id/email")
    public String findIdByEmail(
            @RequestParam String email,
            @RequestParam String name,
            Model model
    ) {
        try {
            String maskedUserName = userService.findUserNameByEmail(email, name);
            LocalDateTime joinDate = userService.getJoinDate(email, name);

            model.addAttribute("foundId", maskedUserName);
            model.addAttribute("joinDate", joinDate);
            model.addAttribute("success", true);

            return "users/findid";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "users/findid";
        }
    }

    /**
     * 전화번호로 아이디 찾기
     */
    @PostMapping("/find-id/phone")
    public String findIdByPhone(
            @RequestParam String phone,
            @RequestParam String name,
            Model model
    ) {
        try {
            String maskedUserName = userService.findUserNameByPhone(phone, name);
            LocalDateTime joinDate = userService.getJoinDate(phone, name);

            model.addAttribute("foundId", maskedUserName);
            model.addAttribute("joinDate", joinDate);
            model.addAttribute("success", true);

            return "users/findid";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "users/findid";
        }
    }

    /**
     * 비밀번호 찾기 페이지
     */
    @GetMapping("/find-password")
    public String findPasswordForm() {
        return "users/findpassword";
    }

    /**
     * 비밀번호 재설정 링크 발송
     */
    @PostMapping("/find-password")
    public String findPassword(
            @RequestParam String username,
            @RequestParam String email,
            Model model
    ) {
        try {
            userService.requestPasswordReset(username, email);
            model.addAttribute("success", true);
            model.addAttribute("email", email);
            return "users/findpassword";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "users/findpassword";
        }
    }

    /**
     * 비밀번호 재설정 페이지
     */
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        if (!userService.isTokenValid(token)) {
            model.addAttribute("error", "유효하지 않거나 만료된 링크입니다.");
            return "users/resetpassword";
        }

        model.addAttribute("token", token);
        return "users/resetpassword";
    }

    /**
     * 비밀번호 재설정 처리
     */
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            Model model
    ) {
        // 비밀번호 일치 확인
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("token", token);
            return "users/resetpassword";
        }

        try {
            userService.resetPassword(token, password);
            model.addAttribute("success", true);
            return "users/resetpassword";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "users/resetpassword";
        }
    }

}
