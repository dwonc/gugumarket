package com.project.gugumarket.controller;

import com.project.gugumarket.entity.Notification;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.NotificationService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 알림 목록 페이지
     */
    @GetMapping
    public String notificationsPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getUser(principal.getName());
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications/notifications";
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(
            @PathVariable Long notificationId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(principal.getName());
            notificationService.markAsRead(notificationId, user);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    @ResponseBody
    public ResponseEntity<?> markAllAsRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(principal.getName());
            notificationService.markAllAsRead(user);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("모든 알림 읽음 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    @ResponseBody
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long notificationId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(principal.getName());
            notificationService.deleteNotification(notificationId, user);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("알림 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 모든 알림 삭제
     */
    @DeleteMapping("/delete-all")
    @ResponseBody
    public ResponseEntity<?> deleteAll(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User user = userService.getUser(principal.getName());
            notificationService.deleteAllNotifications(user);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("모든 알림 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 읽지 않은 알림 개수 조회 (AJAX용)
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<?> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "count", 0));
        }

        try {
            User user = userService.getUser(principal.getName());
            long count = notificationService.getUnreadCount(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "count", 0));
        }
    }
}