package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
public class MypageController {
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    //마이 페이지 조회
    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        if(principal==null){
            return "redirect:/login";
        }
        String userName=principal.getName();
        UserDto userDto=userService.getUserInfo(userName);

        model.addAttribute("user",userDto);
        return "/mypage";
    }
    @PostMapping("/mypage/edit")
    public String editmypage(@ModelAttribute Principal principal, UserDto userDto) {
        if(principal==null){
            return "redirect:/login";
        }
        String userName=principal.getName();
        userService.updateUserInfo(userName,userDto);

        return "redirect:/mypage";
    }
    @PostMapping("/mypage/password")
    public String changePassword(@RequestParam String currentpassword, @RequestParam String newpassword, Principal principal,Model model) {
        if(principal==null) return "redirect:/login";

        String userName=principal.getName();
        boolean success=userService.changePassword(userName,currentpassword,newpassword);
        if(!success){
            model.addAttribute("error","현재 비밀번호가 일치하지 않습니다.");
        }
        model.addAttribute("user",userService.getUserInfo(userName));
        return "/mypage";
    }

}
