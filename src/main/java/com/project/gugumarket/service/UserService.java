package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    @Autowired
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User getUser(String userName) {
        Optional<User> siteUser = this.userRepository.findByUserName(userName);

        if(siteUser.isPresent()) {
            User user = siteUser.get();
            return user;
        }
        else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    // 아이디 중복 체크
    public boolean isUserNameDuplicate(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Transactional
    public User create(UserDto userDto) {
        // 1. 중복 사용자 체크
        if (userRepository.existsByUserName(userDto.getUserName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 검증 (선택사항)
        validatePassword(userDto.getPassword());

        String encodedPassword=passwordEncoder.encode(userDto.getPassword());
        System.out.println("원본 비밀번호: "+userDto.getPassword());
        System.out.println("암호화된 비밀번호: "+encodedPassword);

        // 3. 새 사용자 객체 생성 및 설정
        User user = User.builder()
                .userName(userDto.getUserName())
                .password(encodedPassword)
                .email(userDto.getEmail())
                .nickname(userDto.getNickname())
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .addressDetail(userDto.getAddressDetail())
                .postalCode(userDto.getPostalCode())
                .createdDate(LocalDateTime.now())
                .isActive(true)
                .role("USER")
                .build();

        // 4. 저장 및 반환
        User savedUser=userRepository.save(user);
        System.out.println("DB 저장 완료 - userId: " + savedUser.getUserId());
        return savedUser;
    }

    // 비밀번호 유효성 검증 (선택사항)
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 추가 검증 로직 (영문, 숫자, 특수문자 포함 등)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }
    }
    //내 정보 조회
    public UserDto getUserInfo(String userName) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        UserDto dto=new UserDto();
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }
    //내 정보 수정
    public void updateUserInfo(String userName,UserDto userDto) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        userRepository.save(user);
    }
    //비밀번호 변경
    public boolean changePassword(String userName, String currentpassword,String newpassword) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        if(!passwordEncoder.matches(newpassword,user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newpassword));
        userRepository.save(user);
        return true;
    }

    // 🔥 Principal에서 현재 사용자 가져오기
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String username = principal.getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
}