package com.project.gugumarket.service;

import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("================================");
        System.out.println("로그인 시도 - 사용자: " + username);

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> {
                    System.out.println("사용자를 찾을 수 없음: " + username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        System.out.println("사용자 찾음: " + user.getUserName());
        System.out.println("   - userId: " + user.getUserId());  // ✅ userId 로그 추가
        System.out.println("   - userName: " + user.getUserName());
        System.out.println("   - email: " + user.getEmail());
        System.out.println("   - role: " + user.getRole());
        System.out.println("   - password(암호화): " + user.getPassword());
        System.out.println("   - isActive: " + user.getIsActive());

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        // ✅ CustomUserDetails 반환 (userId 포함!)
        UserDetails userDetails = new CustomUserDetails(
                user.getUserId(),         // ✅ userId 추가
                user.getUserName(),
                user.getPassword(),
                user.getIsActive(),       // enabled
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                true,                     // accountNonLocked
                authorities
        );

        System.out.println("✅ CustomUserDetails 생성 완료 (userId: " + user.getUserId() + ")");
        System.out.println("========================================");

        return userDetails;
    }
}