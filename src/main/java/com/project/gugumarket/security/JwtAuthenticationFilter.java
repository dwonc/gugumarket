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
import org.springframework.web.filter.OncePerRequestFilter; // 모든 요청에 대해 단 한 번만 실행되도록 보장하는 필터 기반 클래스

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j // 로깅을 위한 Lombok 애너테이션
@Component // Spring 빈으로 등록하여 의존성 주입이 가능하도록 설정
@RequiredArgsConstructor // final 필드들을 인자로 받는 생성자를 자동 생성 (의존성 주입을 위함)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider; // JWT 토큰 생성, 검증 등의 유틸리티 제공
    private final CustomUserDetailService customUserDetailService; // 사용자 정보를 DB에서 로드하는 서비스

    // 🔥 필터를 건너뛸 경로들 (인증이 필요 없는 공개된 엔드포인트 목록)
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/api/auth/login", // 로그인 API
            "/api/auth/refresh", // 토큰 재발급 API
            "/api/auth/kakao/**", // 카카오 소셜 로그인 관련 API
            "/api/users/signup", // 회원가입 API
            "/api/users/check-username", // 아이디 중복 확인 API
//            "/api/products/*", // 주석 처리된 경로
//            "/api/products/*/comments",      // ✅ 추가: 댓글 조회 (주석 처리)
            "/api/public/**", // 공개적으로 접근 가능한 API
            "/h2-console/**", // H2 데이터베이스 콘솔 (개발 환경에서 사용)
            "/uploads/**", // 업로드된 파일 접근 경로
            "/images/**", // 이미지 파일 경로
            "/css/**", // CSS 파일 경로
            "/js/**" // JavaScript 파일 경로
    );

    // ✅ 사용자 레벨 조회 경로 정규식 (URL 경로에 숫자가 들어가는 형태를 처리하기 위함)
    private static final Pattern USER_LEVEL_PATTERN = Pattern.compile("^/api/users/\\d+/level$");

    /**
     * 필터 실행 여부를 결정하는 메서드
     * EXCLUDE_URLS 목록에 포함되거나 정규식 패턴에 매칭되면 true (필터 건너뜀) 반환
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher(); // 경로 패턴 매칭 유틸리티

        // ✅ 정규식으로 사용자 레벨 조회 경로 체크
        if (USER_LEVEL_PATTERN.matcher(path).matches()) {
            log.debug("🔓 JWT 필터 건너뜀 (레벨 조회): {}", path);
            return true;
        }

        // EXCLUDE_URLS 목록 중 현재 경로와 일치하는 패턴이 있는지 검사
        boolean shouldExclude = EXCLUDE_URLS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        // 로깅을 통해 필터 실행 여부 확인
        if (shouldExclude) {
            log.debug("🔓 JWT 필터 건너뜀: {}", path);
        } else {
            log.debug("🔒 JWT 필터 실행: {}", path);
        }

        return shouldExclude; // true이면 필터 실행 건너뛰기, false이면 doFilterInternal() 실행
    }

    /**
     * 실제 JWT 인증 로직을 수행하는 메서드
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Request Header에서 JWT 토큰 추출
            String jwt = getJwtFromRequest(request);

            // 2. 토큰 유효성 검증 (토큰이 있고, 유효성 검증을 통과했다면)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // 3. 토큰에서 사용자 식별 정보(username) 추출
                String username = jwtTokenProvider.getUsernameFromToken(jwt);

                // 4. username으로 데이터베이스에서 사용자 상세 정보(UserDetails) 조회
                UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

                // 5. Authentication 객체 생성 (JWT 기반 인증 성공)
                // UsernamePasswordAuthenticationToken은 인증 객체로 사용되며,
                // 첫 번째 인자는 사용자 정보(Principal), 두 번째는 자격 증명(Credentials, JWT에서는 null), 세 번째는 권한 목록을 담음
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                // 요청 정보를 Authentication 객체에 저장 (Web 요청 상세 정보를 포함)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. SecurityContext에 Authentication 설정
                // 현재 스레드의 SecurityContext에 인증 객체를 설정하여,
                // 해당 요청이 인증된 상태임을 Spring Security에 알림
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ JWT 인증 성공: {}", username);
            } else {
                log.debug("⚠️ JWT 토큰 없음 또는 유효하지 않음");
                // 토큰이 없거나 유효하지 않아도 예외를 발생시키지 않고 다음 필터로 넘김
                // (이후 필터나 Security 설정에 의해 접근 권한이 확인될 것임)
            }
        } catch (Exception e) {
            // 인증 처리 과정 중 예상치 못한 예외 발생 시 로깅
            log.error("❌ JWT 인증 실패: {}", e.getMessage());
        }

        // 다음 필터 또는 최종 목적지(Controller)로 요청/응답 전달
        filterChain.doFilter(request, response);
    }

    // Request Header에서 토큰을 추출하는 헬퍼 메서드
    private String getJwtFromRequest(HttpServletRequest request) {
        // "Authorization" 헤더에서 토큰 값을 가져옴
        String bearerToken = request.getHeader("Authorization");

        // 헤더 값이 있고, "Bearer "로 시작하는지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // "Bearer " (7글자)를 제거한 순수 토큰 값만 반환
            return bearerToken.substring(7);
        }

        return null; // 토큰 형식에 맞지 않으면 null 반환
    }

    /**
     * ✅ WebSocket에서 사용할 JWT 인증 메서드
     * HTTP 요청이 아닌, 별도의 WebSocket 세션 연결 시 토큰을 받아 인증 객체를 생성하는 데 사용됨
     */
    public Authentication getAuthentication(String token) {
        try {
            // JWT 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return null; // 유효하지 않으면 null 반환
            }

            // 토큰에서 사용자 식별 정보(username) 추출
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // UserDetails 로드
            UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

            // Authentication 객체 생성 후 반환
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

        } catch (Exception e) {
            // 인증 실패 시 오류 출력 후 null 반환
            System.err.println("JWT 인증 실패: " + e.getMessage());
            return null;
        }
    }
}