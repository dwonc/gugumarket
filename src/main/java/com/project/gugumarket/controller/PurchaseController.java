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
// org.springframework.stereotype.Controller;         // (삭제됨)
// org.springframework.ui.Model;                   // (삭제됨)
import org.springframework.web.bind.annotation.*;    // ★ 변경: RestController까지 포함되도록 사용

import java.security.Principal;
import java.util.Map;

@RestController                                    // ★ 변경: @Controller → @RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final TransactionService transactionService;
    private final ProductService productService;
    private final UserService userService;

    // 구매 페이지 데이터 조회 (이제 HTML이 아니라 JSON으로 내려줌)

    // 🔥 구매 처리
    @PostMapping
    public ResponseEntity<?> createPurchase(@RequestParam Long productId,
                                            @RequestBody PurchaseDto dto,   // ★ 변경: 입금자명만 받던 것 → DTO 전체 JSON으로 받기
                                            Principal principal) {          // ★ 변경: 반환 타입 String → ResponseEntity<?>


        // ★ 추가: 로그인 여부 체크 (principal 자체가 null이면 비로그인 상태)
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "needLogin", true));
        }

        // ★ 추가: 실제 User 엔티티 조회 및 null 방어
        User buyer = userService.getCurrentUser(principal);
        if (buyer == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "needLogin", true));
        }

        // ★ 변경: 예전에는 여기서 새 PurchaseDto를 만들고 depositorName만 세팅했지만,
        //         이제는 프론트에서 받은 dto(depositorName, phone, address, message)를 그대로 전달
        Transaction transaction = transactionService.createTransaction(
                productId, buyer, dto
        );

        // ★ 변경: redirect 문자열 대신 JSON으로 결과 반환
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "transactionId", transaction.getTransactionId()
                )
        );
    }

    // 🔥 구매 완료 정보 조회
    @GetMapping("/complete")
    public ResponseEntity<?> purchaseComplete(@RequestParam Long transactionId) {  // ★ 변경: Model 제거, 반환 타입 변경

        Transaction transaction = transactionService.getTransaction(transactionId);

        // ★ 변경: 뷰 이름("purchase/purchase_complete") 대신 JSON 응답
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "transaction", transaction
                )
        );
    }

    // 입금자명 수정 (기존에도 JSON이었음)
    @PutMapping("/{transactionId}/depositor")
    // @ResponseBody                                      // ★ 변경(삭제): @RestController라 필요 없음
    public ResponseEntity<?> updateDepositor(@PathVariable Long transactionId,
                                             @RequestBody DepositorDto dto) {
        transactionService.updateDepositor(transactionId, dto.getDepositorName());
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    // 거래 취소
    @DeleteMapping("/{transactionId}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
                                               Principal principal) { // ★ 변경: 반환 타입 String → ResponseEntity<?>

        // ★ 추가: 비로그인 상태 방어
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "needLogin", true));
        }

        transactionService.cancelTransaction(transactionId, principal.getName());

        // ★ 변경: redirect:/mypage → JSON 응답
        return ResponseEntity.ok(
                Map.of(
                        "success", true
                )
        );
    }
}



//package com.project.gugumarket.controller;
//
//import com.project.gugumarket.dto.DepositorDto;
//import com.project.gugumarket.dto.PurchaseDto;
//import com.project.gugumarket.entity.Product;
//import com.project.gugumarket.entity.Transaction;
//import com.project.gugumarket.entity.User;
//import com.project.gugumarket.service.ProductService;
//import com.project.gugumarket.service.TransactionService;
//import com.project.gugumarket.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//// org.springframework.stereotype.Controller;         // (삭제됨)
//// org.springframework.ui.Model;                   // (삭제됨)
//import org.springframework.web.bind.annotation.*;    // ★ 변경: RestController까지 포함되도록 사용
//
//import java.security.Principal;
//import java.util.Map;
//
//@RestController                                    // ★ 변경: @Controller → @RestController
//@RequestMapping("/purchase")
//@RequiredArgsConstructor
//public class PurchaseController {
//
//    private final TransactionService transactionService;
//    private final ProductService productService;
//    private final UserService userService;
//
//    // 구매 페이지 데이터 조회 (이제 HTML이 아니라 JSON으로 내려줌)
//    @GetMapping
//    public ResponseEntity<?> purchasePage(@RequestParam Long productId,
//                                          Principal principal) {   // ★ 변경: Model 제거
//
//        Product product = productService.getProduct(productId);
//        User currentUser = userService.getCurrentUser(principal);
//
//        // 🔥 빈 DTO 객체 추가 (원래 코드 그대로)
//        PurchaseDto purchaseDto = new PurchaseDto();
//
//        // ★ 변경: Model에 담아서 뷰 리턴 → JSON 바디로 리턴
//        Map<String, Object> body = Map.of(
//                "user", currentUser,
//                "product", product,
//                "purchaseDto", purchaseDto
//        );
//
//        return ResponseEntity.ok(body);             // ★ 변경: "purchase/purchase" 뷰 이름 → JSON 응답
//    }
//
//    // 🔥 구매 처리
//    @PostMapping
//    public ResponseEntity<?> createPurchase(@RequestParam Long productId,
//                                            @RequestParam String depositorName,
//                                            Principal principal) {  // ★ 변경: 반환 타입 String → ResponseEntity<?>
//
//        User buyer = userService.getCurrentUser(principal);
//
//        PurchaseDto dto = new PurchaseDto();
//        dto.setDepositorName(depositorName);
//
//        Transaction transaction = transactionService.createTransaction(
//                productId, buyer, dto
//        );
//
//        // ★ 변경: redirect 문자열 대신 JSON으로 결과 반환
//        return ResponseEntity.ok(
//                Map.of(
//                        "success", true,
//                        "transactionId", transaction.getTransactionId()
//                )
//        );
//    }
//
//    // 🔥 구매 완료 정보 조회
//    @GetMapping("/complete")
//    public ResponseEntity<?> purchaseComplete(@RequestParam Long transactionId) {  // ★ 변경: Model 제거, 반환 타입 변경
//
//        Transaction transaction = transactionService.getTransaction(transactionId);
//
//        // ★ 변경: 뷰 이름("purchase/purchase_complete") 대신 JSON 응답
//        return ResponseEntity.ok(
//                Map.of(
//                        "success", true,
//                        "transaction", transaction
//                )
//        );
//    }
//
//    // 입금자명 수정 (기존에도 JSON이었음)
//    @PutMapping("/{transactionId}/depositor")
//    // @ResponseBody                                      // ★ 변경(삭제): @RestController라 필요 없음
//    public ResponseEntity<?> updateDepositor(@PathVariable Long transactionId,
//                                             @RequestBody DepositorDto dto) {
//        transactionService.updateDepositor(transactionId, dto.getDepositorName());
//        return ResponseEntity.ok().body(Map.of("success", true));
//    }
//
//    // 거래 취소
//    @DeleteMapping("/{transactionId}/cancel")
//    public ResponseEntity<?> cancelTransaction(@PathVariable Long transactionId,
//                                               Principal principal) { // ★ 변경: 반환 타입 String → ResponseEntity<?>
//
//        transactionService.cancelTransaction(transactionId, principal.getName());
//
//        // ★ 변경: redirect:/mypage → JSON 응답
//        return ResponseEntity.ok(
//                Map.of(
//                        "success", true
//                )
//        );
//    }
//}
