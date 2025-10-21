package com.project.gugumarket.service;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MypageService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserDto getUserInfo(String userName){
        User user  = userRepository.findByUserName(userName)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));

        UserDto dto = new UserDto();
        dto.setUserName(dto.getUserName());
        dto.setEmail(dto.getEmail());
        dto.setPhone(dto.getPhone());
        return dto;
    }

    public void updateUserInfo(String userName,UserDto userDto){
        User user=userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        userRepository.save(user);
    }

    public boolean changePassword(String userName,String currentPassword, String newPassword){
        User user=userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));
        if(!passwordEncoder.matches(currentPassword,user.getPassword())){
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
