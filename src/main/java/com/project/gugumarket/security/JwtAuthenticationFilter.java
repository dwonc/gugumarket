package com.project.gugumarket.security;

import com.project.gugumarket.service.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService customUserDetailService;

    // ✅ 필터를 건너뛸 경로들 (구체적으로 지정!)
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/api/auth/login",          // ✅ 로그인만
            "/api/auth/refresh",        // ✅ 토큰 갱신만
            "/api/users/signup",        // ✅ 회원가입
            "/api/users/check-username", // ✅ 아이디 중복 체크
            "/h2-console/**",
            "/uploads/**",
            "/images/**",
            "/css/**",
            "/js/**"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        boolean shouldExclude = EXCLUDE_URLS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (shouldExclude) {
            log.debug("🔓 JWT 필터 건너뜀: {}", path);
        } else {
            log.debug("🔒 JWT 필터 실행: {}", path);
        }

        return shouldExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Request Header에서 JWT 토큰 추출
            String jwt = getJwtFromRequest(request);

            // 2. 토큰 유효성 검증
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 3. 토큰에서 username 추출
                String username = jwtTokenProvider.getUsernameFromToken(jwt);

                // 4. username으로 사용자 정보 조회
                UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

                // 5. Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. SecurityContext에 Authentication 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ JWT 인증 성공: {}", username);
            } else {
                log.debug("⚠️ JWT 토큰 없음 또는 유효하지 않음");
            }
        } catch (Exception e) {
            log.error("❌ JWT 인증 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // Request Header에서 토큰 추출
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}