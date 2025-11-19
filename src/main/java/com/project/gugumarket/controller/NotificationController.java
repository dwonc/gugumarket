package com.project.gugumarket.controller;

import com.project.gugumarket.dto.NotificationDto;
import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.entity.Notification;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.NotificationService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Map<String, Object>>> getNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            List<Notification> notifications = notificationService.getUserNotifications(user);
            long unreadCount = notificationService.getUnreadCount(user);

            // ✅ Entity → DTO 변환
            List<NotificationDto> notificationDtos = notifications.stream()
                    .map(NotificationDto::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("notifications", notificationDtos);
            result.put("unreadCount", unreadCount);

            return ResponseEntity.ok(ResponseDto.success("알림 목록 조회 성공", result));
        } catch (Exception e) {
            log.error("알림 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getUnreadCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            long count = notificationService.getUnreadCount(user);

            Map<String, Object> result = new HashMap<>();
            result.put("count", count);

            return ResponseEntity.ok(ResponseDto.success("조회 성공", result));
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 개수 조회에 실패했습니다."));
        }
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ResponseDto<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            notificationService.markAsRead(notificationId, user);

            return ResponseEntity.ok(ResponseDto.success("알림을 읽음 처리했습니다."));
        } catch (IllegalArgumentException e) {
            log.error("알림 읽음 처리 실패 - 존재하지 않는 알림: {}", notificationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail("존재하지 않는 알림입니다."));
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ResponseDto<Void>> markAllAsRead(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            notificationService.markAllAsRead(user);

            return ResponseEntity.ok(ResponseDto.success("모든 알림을 읽음 처리했습니다."));
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ResponseDto<Void>> deleteNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            notificationService.deleteNotification(notificationId, user);

            return ResponseEntity.ok(ResponseDto.success("알림이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("알림 삭제 실패 - 존재하지 않는 알림: {}", notificationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail("존재하지 않는 알림입니다."));
        } catch (Exception e) {
            log.error("알림 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 삭제에 실패했습니다."));
        }
    }

    /**
     * 모든 알림 삭제
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<ResponseDto<Void>> deleteAll(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDto.fail("로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(authentication.getName());
            notificationService.deleteAllNotifications(user);

            return ResponseEntity.ok(ResponseDto.success("모든 알림이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("모든 알림 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("알림 삭제에 실패했습니다."));
        }
    }
}