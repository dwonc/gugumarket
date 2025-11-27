package com.project.gugumarket.security;

import com.project.gugumarket.service.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService customUserDetailService;

    // 🔥 필터를 건너뛸 경로들
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/kakao/**",
            "/api/users/signup",
            "/api/users/check-username",
//            "/api/products/*",
//            "/api/products/*/comments",      // ✅ 추가: 댓글 조회
            "/api/public/**",
            "/h2-console/**",
            "/uploads/**",
            "/images/**",
            "/css/**",
            "/js/**"
    );

    // ✅ 사용자 레벨 조회 경로 정규식
    private static final Pattern USER_LEVEL_PATTERN = Pattern.compile("^/api/users/\\d+/level$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        // ✅ 정규식으로 사용자 레벨 조회 체크
        if (USER_LEVEL_PATTERN.matcher(path).matches()) {
            log.debug("🔓 JWT 필터 건너뜀 (레벨 조회): {}", path);
            return true;
        }

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

    /**
     * ✅ WebSocket에서 사용할 JWT 인증 메서드
     */
    public Authentication getAuthentication(String token) {
        try {
            // JWT 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return null;
            }

            // 토큰에서 사용자 정보 추출
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // UserDetails 로드
            UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

            // Authentication 객체 생성
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

        } catch (Exception e) {
            System.err.println("JWT 인증 실패: " + e.getMessage());
            return null;
        }
    }
}