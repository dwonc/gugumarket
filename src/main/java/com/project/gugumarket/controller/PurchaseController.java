package com.project.gugumarket.controller;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.KakaoPayService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.TransactionService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final TransactionService transactionService;
    private final ProductService productService;
    private final UserService userService;
    private final KakaoPayService kakaoPayService;  // ⭐ 추가

    // =============== 1. 구매 페이지 진입 ======================
    @GetMapping("/ready")
    public ResponseEntity<?> purchaseReady(@RequestParam Long productId,
                                           Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(
                    Map.of("success", false, "message", "로그인이 필요합니다.")
            );
        }

        User buyer = userService.getCurrentUser(principal);

        Product product = productService.getProduct(productId);
        if (product == null) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "상품을 찾을 수 없습니다.")
            );
        }

        // 필요한 필드만 골라서 Map으로 구성
        Map<String, Object> productMap = new HashMap<>();
        productMap.put("productId", product.getProductId());
        productMap.put("title", product.getTitle());
        productMap.put("price", product.getPrice());
        productMap.put("mainImage", product.getMainImage());
        productMap.put("bankName", product.getBankName());
        productMap.put("accountNumber", product.getAccountNumber());
        productMap.put("accountHolder", product.getAccountHolder());
        productMap.put("sellerNickname", product.getSeller().getNickname());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", buyer.getUserId());
        userMap.put("nickname", buyer.getNickname());
        userMap.put("phone", buyer.getPhone());
        userMap.put("address", buyer.getAddress());

        Map<String, Object> data = Map.of(
                "product", productMap,
                "user", userMap
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", data
                )
        );
    }

    // =============== 2. 구매 생성 - ⭐ 통합 버전 ============================
    @PostMapping
    public ResponseEntity<?> createPurchase(@RequestBody PurchaseDto dto,
                                            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(
                    Map.of("success", false, "message", "로그인이 필요합니다.")
            );
        }

        if (dto.getProductId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "productId가 필요합니다.")
            );
        }

        User buyer = userService.getCurrentUser(principal);

        // ⭐ 결제 수단 확인 (기본값: BANK_TRANSFER)
        String paymentMethod = dto.getPaymentMethod() != null
                ? dto.getPaymentMethod()
                : "BANK_TRANSFER";

        // 1. ⭐ 거래 생성 (통합 - User 객체로 통일!)
        Transaction transaction = transactionService.createTransaction(buyer, dto);

        // 2. ⭐ 카카오페이면 결제 준비
        if ("KAKAOPAY".equals(paymentMethod)) {
            try {
                KakaoPayReadyResponse kakaoPayReady = kakaoPayService.kakaoPayReady(
                        transaction.getTransactionId(),
                        buyer.getUserId()
                );

                Map<String, Object> data = Map.of(
                        "transactionId", transaction.getTransactionId(),
                        "kakaoPayUrl", kakaoPayReady.getNextRedirectPcUrl(),
                        "mobileUrl", kakaoPayReady.getNextRedirectMobileUrl()
                );

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "paymentMethod", "KAKAOPAY",
                                "data", data
                        )
                );
            } catch (Exception e) {
                return ResponseEntity.status(500).body(
                        Map.of(
                                "success", false,
                                "message", "카카오페이 결제 준비 중 오류가 발생했습니다: " + e.getMessage()
                        )
                );
            }
        }

        // 3. ✅ 무통장 입금 - 기존 응답
        Map<String, Object> data = Map.of(
                "transactionId", transaction.getTransactionId()
        );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "paymentMethod", "BANK_TRANSFER",
                        "data", data
                )
        );
    }

    // =============== 3. 구매 완료 페이지 조회 =================
    @GetMapping("/complete")
    public ResponseEntity<?> purchaseComplete(@RequestParam Long transactionId) {

        Transaction t = transactionService.getTransaction(transactionId);
        if (t == null) {
            return ResponseEntity.status(404).body(
                    Map.of("success", false, "message", "거래 정보를 찾을 수 없습니다.")
            );
        }

        Product product = t.getProduct();
        User buyer = t.getBuyer();
        User seller = t.getSeller();

        Map<String, Object> productMap = new HashMap<>();
        productMap.put("productId", product.getProductId());
        productMap.put("title", product.getTitle());
        productMap.put("price", product.getPrice());
        productMap.put("mainImage", product.getMainImage());
        productMap.put("bankName", product.getBankName());
        productMap.put("accountNumber", product.getAccountNumber());
        productMap.put("accountHolder", product.getAccountHolder());
        productMap.put("sellerNickname", seller.getNickname());

        Map<String, Object> buyerMap = new HashMap<>();
        buyerMap.put("userId", buyer.getUserId());
        buyerMap.put("nickname", buyer.getNickname());
        buyerMap.put("phone", buyer.getPhone());
        buyerMap.put("address", buyer.getAddress());

        Map<String, Object> sellerMap = new HashMap<>();
        sellerMap.put("userId", seller.getUserId());
        sellerMap.put("nickname", seller.getNickname());
        sellerMap.put("phone", seller.getPhone());
        sellerMap.put("address", seller.getAddress());

        Map<String, Object> transactionMap = new HashMap<>();
        transactionMap.put("transactionId", t.getTransactionId());
        transactionMap.put("product", productMap);
        transactionMap.put("buyer", buyerMap);
        transactionMap.put("seller", sellerMap);

        Map<String, Object> data = Map.of("transaction", transactionMap);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", data
                )
        );
    }

    // Path 방식도 지원
    @GetMapping("/{transactionId}")
    public ResponseEntity<?> purchaseCompleteByPath(@PathVariable Long transactionId) {
        return purchaseComplete(transactionId);
    }

    // =============== 4. 입금자명 수정 =======================
    @PutMapping("/{transactionId}/depositor")
    public ResponseEntity<?> updateDepositor(@PathVariable Long transactionId,
                                             @RequestBody DepositorDto dto) {

        transactionService.updateDepositor(transactionId, dto.getDepositorName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // =============== 5. 거래 취소 ===========================
    @DeleteMapping("/{transactionId}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
                                               Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(
                    Map.of("success", false, "message", "로그인이 필요합니다.")
            );
        }

        transactionService.cancelTransaction(transactionId, principal.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }
}