package com.project.gugumarket.controller;

import com.project.gugumarket.dto.TransactionDetailDto;
import com.project.gugumarket.dto.UserLevelDto;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.TransactionService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TransactionController {

    private final UserService userService;
    private final TransactionService transactionService;

    /**
     * 거래 상세 조회
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<?> transactionDetail(@PathVariable Long transactionId,
                                               Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            Transaction transaction = transactionService.getTransaction(transactionId);
            User user = userService.getUser(principal.getName());

            boolean isSeller = transaction.getSeller().getUserId().equals(user.getUserId());
            boolean isBuyer  = transaction.getBuyer().getUserId().equals(user.getUserId());

            if (!isSeller && !isBuyer) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "권한이 없습니다."
                        ));
            }

            TransactionDetailDto dto = TransactionDetailDto.fromEntity(transaction);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction", dto);
            response.put("isSeller", isSeller);
            response.put("isBuyer", isBuyer);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("거래 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("거래 조회 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "거래 조회 중 오류가 발생했습니다."
                    ));
        }
    }

    /**
     * 거래 완료 처리 (판매자) + 등급 정보 반환
     */
    @PostMapping("/transactions/{transactionId}/complete")
    public ResponseEntity<?> completeTransaction(@PathVariable Long transactionId,
                                                 Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            User seller = userService.getUser(principal.getName());

            transactionService.completeTransaction(transactionId, seller);

            // 🆕 거래 완료 후 판매자의 최신 등급 정보 반환
            User updatedSeller = userService.getUser(principal.getName());
            UserLevelDto levelInfo = UserLevelDto.from(updatedSeller);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "거래가 완료되었습니다! 🎉",
                    "levelInfo", levelInfo  // 🆕 등급 정보
            ));
        } catch (IllegalArgumentException e) {
            log.error("거래 완료 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("거래 완료 처리 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "거래 완료 처리 중 오류가 발생했습니다."
                    ));
        }
    }

    /**
     * 입금자명 수정 (구매자)
     */
    @PostMapping("/transactions/{transactionId}/depositor")
    public ResponseEntity<?> updateDepositor(@PathVariable Long transactionId,
                                             @RequestBody Map<String,String> request,
                                             Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            String depositorName = request.get("depositorName");

            if (depositorName == null || depositorName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "입금자명을 입력해주세요."
                        ));
            }

            transactionService.updateDepositor(transactionId, depositorName.trim());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "입금자명이 수정되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            log.error("입금자명 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("입금자명 수정 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "입금자명 수정 중 오류가 발생했습니다."
                    ));
        }
    }

    /**
     * 거래 취소 (구매자)
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
                                               Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            transactionService.cancelTransaction(transactionId, principal.getName());

            log.info("거래 취소 완료 - 거래 ID: {}", transactionId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "거래가 취소되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            log.error("거래 취소 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "거래 취소 실패: " + e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("거래 취소 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "거래 취소 중 오류가 발생했습니다."
                    ));
        }
    }
}