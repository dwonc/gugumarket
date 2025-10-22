package com.project.gugumarket.controller;


import com.project.gugumarket.dto.DepositorDto;
import com.project.gugumarket.dto.PurchaseDto;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.TransactionService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final TransactionService transactionService;
    private final ProductService productService;
    private final UserService userService;

    // 구매 페이지 표시
    @GetMapping
    public String purchasePage(@RequestParam Long productId,
                               Model model,
                               Principal principal) {
        Product product = productService.getProduct(productId);
        User currentUser = userService.getCurrentUser(principal);

        // 🔥 빈 DTO 객체 추가
        PurchaseDto purchaseDto = new PurchaseDto();

        model.addAttribute("user", currentUser);
        model.addAttribute("product", product);
        model.addAttribute("purchaseDto", purchaseDto); // 🔥 이게 중요!

        return "purchase/purchase";
    }

    // 🔥 구매 처리
    @PostMapping
    public String createPurchase(@RequestParam Long productId,
                                 @RequestParam String depositorName,
                                 Principal principal) {
        User buyer = userService.getCurrentUser(principal);

        PurchaseDto dto = new PurchaseDto();
        dto.setDepositorName(depositorName);

        Transaction transaction = transactionService.createTransaction(
                productId, buyer, dto
        );

        return "redirect:/purchase/complete?transactionId=" + transaction.getTransactionId();
    }

    // 🔥 구매 완료 페이지
    @GetMapping("/complete")
    public String purchaseComplete(@RequestParam Long transactionId,
                                   Model model) {
        Transaction transaction = transactionService.getTransaction(transactionId);
        model.addAttribute("transaction", transaction);
        return "purchase/purchase_complete";
    }

    // 입금자명 수정
    @PutMapping("/{transactionId}/depositor")
    @ResponseBody
    public ResponseEntity<?> updateDepositor(@PathVariable Long transactionId,
                                             @RequestBody DepositorDto dto) {
        transactionService.updateDepositor(transactionId, dto.getDepositorName());
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    // 거래 취소
    @DeleteMapping("/{transactionId}/cancel")
    public String cancelTransaction(@PathVariable Long transactionId,
                                    Principal principal) {
        transactionService.cancelTransaction(transactionId, principal.getName());
        return "redirect:/mypage";
    }
}