package com.project.gugumarket.controller;

import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/test/user")
    public String testUser(@RequestParam String username) {
        Optional<User> userOpt = userRepository.findByUserName(username);

        if (userOpt.isEmpty()) {
            return "❌ 사용자를 찾을 수 없음: " + username;
        }

        User user = userOpt.get();
        return "✅ 사용자 찾음: " + user.getUserName() +
                "<br>이메일: " + user.getEmail() +
                "<br>암호화된 비밀번호: " + user.getPassword() +
                "<br>역할: " + user.getRole();
    }

    @GetMapping("/test/password")
    public String testPassword(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOpt = userRepository.findByUserName(username);

        if (userOpt.isEmpty()) {
            return "❌ 사용자를 찾을 수 없음";
        }

        User user = userOpt.get();
        boolean matches = passwordEncoder.matches(password, user.getPassword());

        return "입력한 비밀번호: " + password +
                "<br>DB의 암호화된 비밀번호: " + user.getPassword() +
                "<br>매칭 결과: " + (matches ? "✅ 일치" : "❌ 불일치");
    }
}
