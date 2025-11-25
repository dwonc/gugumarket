package com.project.gugumarket.controller;

import com.project.gugumarket.dto.KakaoPayApproveResponse;
import com.project.gugumarket.dto.KakaoPayReadyResponse;
import com.project.gugumarket.service.KakaoPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오페이 결제 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final KakaoPayService kakaoPayService;

    /**
     * 카카오페이 결제 준비 (Step 1)
     * @param transactionId 거래 ID
     * @param authentication 인증 정보
     * @return 결제 준비 응답 (결제 URL 포함)
     */
    @PostMapping("/kakaopay/ready/{transactionId}")
    public ResponseEntity<?> kakaoPayReady(
            @PathVariable Long transactionId,
            Authentication authentication
    ) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("카카오페이 결제 준비 요청 - userId: {}, transactionId: {}", userId, transactionId);

            // 결제 준비 요청
            KakaoPayReadyResponse readyResponse = kakaoPayService.kakaoPayReady(transactionId, userId);

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tid", readyResponse.getTid());
            response.put("next_redirect_pc_url", readyResponse.getNextRedirectPcUrl());
            response.put("next_redirect_mobile_url", readyResponse.getNextRedirectMobileUrl());

            log.info("카카오페이 결제 준비 완료 - tid: {}", readyResponse.getTid());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("카카오페이 결제 준비 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            log.error("카카오페이 결제 준비 실패", e);
            return ResponseEntity.internalServerError().body(
                    Map.of("success", false, "message", "결제 준비 중 오류가 발생했습니다.")
            );
        }
    }

    /**
     * 카카오페이 결제 승인 (Step 2 - 콜백)
     * @param pgToken 카카오페이 pg_token
     * @param transactionId 거래 ID
     * @return 결제 승인 응답
     */
    @GetMapping("/kakaopay/success")
    public ResponseEntity<?> kakaoPaySuccess(
            @RequestParam("pg_token") String pgToken,
            @RequestParam("transaction_id") Long transactionId
    ) {
        try {
            log.info("카카오페이 결제 승인 요청 - transactionId: {}, pgToken: {}", transactionId, pgToken);

            // 결제 승인 요청
            KakaoPayApproveResponse approveResponse = kakaoPayService.kakaoPayApprove(transactionId, pgToken);

            log.info("카카오페이 결제 승인 완료 - tid: {}, aid: {}",
                    approveResponse.getTid(), approveResponse.getAid());

            // 프론트엔드 리다이렉트 URL
            String redirectUrl = String.format(
                    "http://localhost:3000/payment/success?transaction_id=%d&payment_method=KAKAOPAY",
                    transactionId
            );

            // HTML 응답으로 리다이렉트
            String html = String.format(
                    "<html><body><script>window.location.href='%s';</script></body></html>",
                    redirectUrl
            );

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (Exception e) {
            log.error("카카오페이 결제 승인 실패", e);
            String errorUrl = String.format(
                    "http://localhost:3000/payment/fail?transaction_id=%d&message=%s",
                    transactionId, "결제 승인 중 오류가 발생했습니다."
            );

            String html = String.format(
                    "<html><body><script>window.location.href='%s';</script></body></html>",
                    errorUrl
            );

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }

    /**
     * 카카오페이 결제 취소 콜백
     * @param transactionId 거래 ID
     * @return 리다이렉트
     */
    @GetMapping("/kakaopay/cancel")
    public ResponseEntity<?> kakaoPayCancel(@RequestParam("transaction_id") Long transactionId) {
        log.info("카카오페이 결제 취소 - transactionId: {}", transactionId);

        String redirectUrl = String.format(
                "http://localhost:3000/payment/cancel?transaction_id=%d",
                transactionId
        );

        String html = String.format(
                "<html><body><script>window.location.href='%s';</script></body></html>",
                redirectUrl
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /**
     * 카카오페이 결제 실패 콜백
     * @param transactionId 거래 ID
     * @return 리다이렉트
     */
    @GetMapping("/kakaopay/fail")
    public ResponseEntity<?> kakaoPayFail(@RequestParam("transaction_id") Long transactionId) {
        log.info("카카오페이 결제 실패 - transactionId: {}", transactionId);

        String redirectUrl = String.format(
                "http://localhost:3000/payment/fail?transaction_id=%d&message=%s",
                transactionId, "결제에 실패했습니다."
        );

        String html = String.format(
                "<html><body><script>window.location.href='%s';</script></body></html>",
                redirectUrl
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /**
     * 결제 상태 조회
     * @param transactionId 거래 ID
     * @param authentication 인증 정보
     * @return 결제 상태
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable Long transactionId,
            Authentication authentication
    ) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("결제 상태 조회 - userId: {}, transactionId: {}", userId, transactionId);

            // TODO: TransactionService에서 상태 조회
            // Transaction transaction = transactionService.getTransaction(transactionId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            // response.put("status", transaction.getStatus());
            // response.put("paymentMethod", transaction.getPaymentMethod());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("결제 상태 조회 실패", e);
            return ResponseEntity.internalServerError().body(
                    Map.of("success", false, "message", "결제 상태 조회 중 오류가 발생했습니다.")
            );
        }
    }
}