package com.project.gugumarket.controller;

import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;

    public MainController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 메인 페이지
    @GetMapping("/main")
    public String main(Model model) {
        // 현재 로그인한 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        System.out.println("로그인 성공 - 사용자: " + username);

        model.addAttribute("username", username);
        // 사용자 정보를 DB에서 가져오기
        Optional<User> userOpt = userRepository.findByUserName(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("username", user.getUserName());
            System.out.println("✅ 사용자 정보 로드 완료: " + user.getNickname());
        }


        return "main";
    }

    // 홈 페이지 (로그인 전)
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
