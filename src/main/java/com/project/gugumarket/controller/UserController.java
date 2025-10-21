package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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

}
