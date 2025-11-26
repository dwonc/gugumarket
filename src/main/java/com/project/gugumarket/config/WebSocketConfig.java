package com.project.gugumarket.config;

import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.CustomUserDetails;
import com.project.gugumarket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

/**
 * WebSocket 설정
 * - STOMP over WebSocket 사용
 * - JWT 인증 통합 (CustomUserDetails 사용)
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository; // ✅ 추가

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 prefix
        config.setApplicationDestinationPrefixes("/app");

        // 특정 사용자에게 메시지 전송 시 사용할 prefix
        config.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 설정
                .withSockJS();  // SockJS fallback 지원
    }

    /**
     * JWT 인증을 위한 채널 인터셉터 설정
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // WebSocket 연결 시 JWT 토큰 검증
                    String authToken = accessor.getFirstNativeHeader("Authorization");

                    if (authToken != null && authToken.startsWith("Bearer ")) {
                        String token = authToken.substring(7);

                        try {
                            // ✅ JWT 검증
                            if (jwtTokenProvider.validateToken(token)) {

                                // ✅ 토큰에서 userId 추출
                                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                                // ✅ DB에서 실제 User 조회 (userName 통일 용도)
                                User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                                String principalName = user.getUserName(); // 🔥 WebSocket principal = userName

                                // ✅ CustomUserDetails 생성 (username = userName 으로 고정)
                                CustomUserDetails userDetails = new CustomUserDetails(
                                        user.getUserId(),               // userId
                                        principalName,                  // username (principal name)
                                        "",                             // password (필요 없음)
                                        true,
                                        true,
                                        true,
                                        true,
                                        Collections.singletonList(
                                                new SimpleGrantedAuthority("ROLE_USER")
                                        )
                                );

                                // ✅ Authentication 객체 생성
                                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                                // 여기서 authentication.getName() == principalName == user.getUserName()
                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);

                                System.out.println("✅ WebSocket 인증 성공 - userId: "
                                        + user.getUserId() + ", principalName(userName): " + principalName);
                            }
                        } catch (Exception e) {
                            System.err.println("❌ WebSocket JWT 인증 실패: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                return message;
            }
        });
    }
}