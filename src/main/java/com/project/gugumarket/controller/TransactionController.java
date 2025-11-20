package com.project.gugumarket.controller;

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
public class TransactionController {

    private final UserService userService;
    private final TransactionService transactionService;
    /**
     * 거래 상세 페이지
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<?> transactionDetail(@PathVariable Long transactionId, Principal principal) {
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

            // 구매자나 판매자만 볼 수 있음
            if (!transaction.getBuyer().getUserId().equals(user.getUserId()) &&
                    !transaction.getSeller().getUserId().equals(user.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "success", false,
                        "message", "권한이 없습니다."
                ));
    }
            // 현재 사용자가 판매자인지 구매자인지 확인
            boolean isSeller = transaction.getSeller().getUserId().equals(user.getUserId());
            boolean isBuyer = transaction.getBuyer().getUserId().equals(user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction", transaction);
            response.put("user", user);
            response.put("isSeller", isSeller);
            response.put("isBuyer", isBuyer);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("거래 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "거래 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 거래 완료 처리 (판매자가 완료 버튼 클릭)
     */
    @PostMapping("/transactions/{transactionId}/complete")
    public ResponseEntity<?> completeTransaction(
            @PathVariable Long transactionId,
            Principal principal) {

        if (principal == null) {    //로그인 확인
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            //판매자 정보 조회
            User seller = userService.getUser(principal.getName());

            //거래 완료 처리
            transactionService.completeTransaction(transactionId, seller);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "거래가 완료되었습니다."
            ));
        } catch (Exception e) {
            log.error("거래 완료 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * 입금자명 수정
     */
    @PostMapping("/transactions/{transactionId}/depositor")
    public ResponseEntity<?> updateDepositor(
            @PathVariable Long transactionId,
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
            String depositorName = request.get("depositorName");    //입금자명 가져오기

            if (depositorName == null || depositorName.trim().isEmpty()) {
                
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "입금자명을 입력해주세요."
                        ));
            }

            transactionService.updateDepositor(transactionId, depositorName);   //입금자명 수정
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "입금자명이 수정되었습니다."
            ));
        } catch (Exception e) {
            log.error("입금자명 수정 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 거래 취소
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
                    // 거래 취소 (기존과 동일)
                    transactionService.cancelTransaction(transactionId, principal.getName());
                    
                    log.info("거래 취소 완료 - 거래 ID: {}", transactionId);
        
                    // ⭐ redirect 대신 JSON 응답
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "거래가 취소되었습니다."
                    ));
        
                } catch (Exception e) {
                    log.error("거래 취소 실패: {}", e.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "success", false,
                                    "message", "거래 취소 실패: " + e.getMessage()
                            ));
                }
            }
        }
