package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.dto.LoginDto;
import com.project.gugumarket.dto.LoginResponse;
import com.project.gugumarket.dto.UserResponseDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.JwtTokenProvider;
import com.project.gugumarket.service.CustomUserDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CustomUserDetailService customUserDetailService;  // ✅ 추가

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponse>> login(@Valid @RequestBody LoginDto loginDto) {
        try {
            log.info("🔐 로그인 시도: {}", loginDto.getUserName());

            // 1. 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUserName(),
                            loginDto.getPassword()
                    )
            );

            // 2. SecurityContext에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginDto.getUserName());

            // 4. 사용자 정보 조회
            User user = userRepository.findByUserName(loginDto.getUserName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 5. 응답 생성
            LoginResponse loginResponse = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .username(user.getUserName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();

            log.info("✅ 로그인 성공: {}", loginDto.getUserName());

            return ResponseEntity.ok(ResponseDto.success("로그인 성공", loginResponse));

        } catch (AuthenticationException e) {
            log.error("❌ 로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseDto.fail("아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    /**
     * 토큰 갱신 API
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<LoginResponse>> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        try {
            // Bearer 제거 (있으면)
            String token = refreshToken;
            if (refreshToken.startsWith("Bearer ")) {
                token = refreshToken.substring(7);
            }

            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.badRequest()
                        .body(ResponseDto.fail("유효하지 않은 Refresh Token입니다."));
            }

            // 사용자명 추출
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // ✅ UserDetails 로드
            UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

            // ✅ 올바른 Authentication 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            // 새로운 토큰 생성
            String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            // 사용자 정보 조회
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 응답 생성
            LoginResponse response = LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .username(user.getUserName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();

            log.info("✅ 토큰 갱신 성공: {}", username);

            return ResponseEntity.ok(ResponseDto.success("토큰 갱신 성공", response));

        } catch (Exception e) {
            log.error("❌ 토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ResponseDto.fail("토큰 갱신에 실패했습니다."));
        }
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout() {
        SecurityContextHolder.clearContext();
        log.info("✅ 로그아웃 성공");
        return ResponseEntity.ok(ResponseDto.success("로그아웃 성공"));
    }

    /**
     * 현재 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public ResponseEntity<ResponseDto<UserResponseDto>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.badRequest()
                    .body(ResponseDto.fail("인증되지 않은 사용자입니다."));
        }

        String username = authentication.getName();
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ✅ Entity → DTO 변환
        UserResponseDto userDto = UserResponseDto.fromEntity(user);

        return ResponseEntity.ok(ResponseDto.success("조회 성공", userDto));
    }
}