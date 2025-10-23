package com.project.gugumarket.controller;

import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.TransactionService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class TransactionController {
    // 📍 TransactionController.java에 추가할 코드

    private final UserService userService;
    private final TransactionService transactionService;
    /**
     * 거래 상세 페이지
     */
    @GetMapping("/transaction/{transactionId}")
    public String transactionDetail(@PathVariable Long transactionId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Transaction transaction = transactionService.getTransaction(transactionId);
            User user = userService.getUser(principal.getName());

            // 구매자나 판매자만 볼 수 있음
            if (!transaction.getBuyer().getUserId().equals(user.getUserId()) &&
                    !transaction.getSeller().getUserId().equals(user.getUserId())) {
                return "redirect:/";
            }

            // 현재 사용자가 판매자인지 구매자인지 확인
            boolean isSeller = transaction.getSeller().getUserId().equals(user.getUserId());
            boolean isBuyer = transaction.getBuyer().getUserId().equals(user.getUserId());

            model.addAttribute("transaction", transaction);
            model.addAttribute("user", user);
            model.addAttribute("isSeller", isSeller);
            model.addAttribute("isBuyer", isBuyer);

            return "purchase/transaction_detail";
        } catch (Exception e) {
            log.error("거래 조회 실패: {}", e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * 🔥 거래 완료 처리 (판매자가 완료 버튼 클릭)
     */
    @PostMapping("/transaction/{transactionId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeTransaction(
            @PathVariable Long transactionId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            User seller = userService.getUser(principal.getName());
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
    @PostMapping("/transaction/{transactionId}/depositor")
    @ResponseBody
    public ResponseEntity<?> updateDepositor(
            @PathVariable Long transactionId,
            @RequestParam String depositorName,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            transactionService.updateDepositor(transactionId, depositorName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "입금자명이 수정되었습니다."
            ));
        } catch (Exception e) {
            log.error("입금자명 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * 거래 취소
     */
    @PostMapping("/transaction/{transactionId}/cancel")
    public String cancelTransaction(@PathVariable Long transactionId, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            transactionService.cancelTransaction(transactionId, principal.getName());
            log.info("거래 취소 완료 - 거래 ID: {}", transactionId);
            return "redirect:/mypage";
        } catch (Exception e) {
            log.error("거래 취소 실패: {}", e.getMessage());
            return "redirect:/transaction/" + transactionId + "?error=" + e.getMessage();
        }
    }
}
