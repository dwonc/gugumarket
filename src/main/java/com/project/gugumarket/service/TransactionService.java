package com.project.gugumarket.service;

import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.TransactionStatus;
import com.project.gugumarket.dto.PurchaseDto;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.TransactionRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j  // 🔥 로깅 추가
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;  // 🔥 알림 서비스 추가

    /**
     * 거래 생성 - 통합 버전 (모든 결제 수단)
     * ⭐ User 객체로 통일!
     */
    @Transactional
    public Transaction createTransaction(User buyer, PurchaseDto dto) {
        // 상품 조회
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 이미 판매된 상품인지 확인
        if (product.getStatus() == ProductStatus.SOLD_OUT) {
            throw new IllegalArgumentException("이미 판매완료된 상품입니다");
        }

        // 자기 자신의 상품인지 확인
        if (product.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalStateException("자신의 상품은 구매할 수 없습니다.");
        }

        // 결제 수단 결정 (기본값: 무통장 입금)
        String paymentMethod = dto.getPaymentMethod() != null
                ? dto.getPaymentMethod()
                : "BANK_TRANSFER";

        // 거래 생성
        Transaction transaction = Transaction.builder()
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .paidAmount(product.getPrice())
                .depositorName(dto.getDepositorName())  // 무통장 입금용 (선택)
                .paymentMethod(paymentMethod)
                .status(TransactionStatus.PENDING)
                .transactionDate(LocalDateTime.now())
                .build();

        product.updateStatus(ProductStatus.RESERVED);
        productRepository.save(product);

        Transaction saved = transactionRepository.save(transaction);

        try {
            notificationService.createPurchaseNotification(saved);
            log.info("구매 알림 생성 완료 - 거래 ID: {}, 구매자: {}, 판매자: {}",
                    saved.getTransactionId(),
                    buyer.getNickname(),
                    product.getSeller().getNickname());
        } catch (Exception e) {
            log.error("구매 알림 생성 실패 - 거래 ID: {}, 오류: {}",
                    saved.getTransactionId(), e.getMessage());
        }

        return saved;
    }

    /**
     * 거래 조회 (단일)
     */
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));
    }

    /**
     * 거래 조회 with 권한 체크
     */
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long transactionId, Long userId) {
        Transaction transaction = getTransaction(transactionId);

        // 구매자 또는 판매자인지 확인
        if (!transaction.getBuyer().getUserId().equals(userId)
                && !transaction.getSeller().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 거래에 접근할 권한이 없습니다.");
        }

        return transaction;
    }


    // 입금자명 수정
    @Transactional
    public void updateDepositor(Long transactionId, String depositorName) {
        Transaction transaction = getTransaction(transactionId);
        transaction.updateDepositor(depositorName);
        transactionRepository.save(transaction);

        log.info("입금자명 수정 완료 - 거래 ID: {}, 입금자명: {}", transactionId, depositorName);
    }

    /**
     * 카카오페이 TID 저장 (결제 준비 후)
     */
    @Transactional
    public void updateKakaoPayTid(Long transactionId, String tid) {
        Transaction transaction = getTransaction(transactionId);

        transaction.setTid(tid);
        transaction.setStatus(TransactionStatus.PENDING);
        transactionRepository.save(transaction);

        log.info("카카오페이 TID 저장 완료 - transactionId: {}, tid: {}", transactionId, tid);
    }

    /**
     * 거래 취소
     */
    @Transactional
    public void cancelTransaction(Long transactionId, String username) {
        Transaction transaction = getTransaction(transactionId);

        // 이미 완료된 거래는 취소 불가
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalStateException("완료된 거래는 취소할 수 없습니다.");
        }
        if (!transaction.getBuyer().getUserName().equals(username)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        // 상품 상태 원복
        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SALE);
        productRepository.save(product);

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCancelReason("사용자 취소");
        transaction.setCancelledAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        log.info("거래 취소 - transactionId: {}, username: {}", transactionId, username);
    }

    /**
     * 카카오페이 결제 승인 + 가짜 정산 처리
     */
    @Transactional
    public void completeKakaoPayment(Long transactionId, String aid, String paymentMethodType) {
        Transaction transaction = getTransaction(transactionId);
        Product product = transaction.getProduct();

        // 1. 거래 상태 업데이트
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setKakaoPayAid(aid);
        transaction.setPaymentMethodType(paymentMethodType);
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        // 2. 🎬 가짜 정산 처리
        transaction.setSettled(true);
        transaction.setSettledAt(LocalDateTime.now());
        transaction.setSettlementAmount(product.getPrice());

        // 3. 상품 상태 업데이트
        product.setStatus(ProductStatus.SOLD_OUT);

        transactionRepository.save(transaction);
        productRepository.save(product);

        log.info("카카오페이 결제 및 정산 완료 (시뮬레이션) - transactionId: {}, 정산금액: {}원",
                transactionId, product.getPrice());

        // 4. 거래 완료 알림
        notificationService.createTransactionCompleteNotification(transaction);
    }

    // 🆕🆕🆕 회원 등급 업데이트 로직 추가 🆕🆕🆕
    @Transactional
    public void completeTransaction(Long transactionId, User seller) {
        Transaction transaction = getTransaction(transactionId);

        if (!transaction.getSeller().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("판매자만 거래를 완료할 수 있습니다");
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("완료할 수 없는 거래 상태입니다");
        }

        Transaction updatedTransaction = Transaction.builder()
                .transactionId(transaction.getTransactionId())
                .product(transaction.getProduct())
                .buyer(transaction.getBuyer())
                .seller(transaction.getSeller())
                .depositorName(transaction.getDepositorName())
                .status(TransactionStatus.COMPLETED)
                .transactionDate(LocalDateTime.now())
                .createdDate(transaction.getCreatedDate())
                .build();

        transactionRepository.save(updatedTransaction);

        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SOLD_OUT);
        productRepository.save(product);

        // 🔥🔥🔥 회원 등급 업데이트 (판매자 & 구매자) 🔥🔥🔥
        User sellerUser = transaction.getSeller();
        User buyerUser = transaction.getBuyer();

        sellerUser.completeTransaction();  // 거래 횟수 +1, 등급 자동 업데이트
        buyerUser.completeTransaction();   // 거래 횟수 +1, 등급 자동 업데이트

        userRepository.save(sellerUser);
        userRepository.save(buyerUser);

        log.info("✅ 거래 완료 + 등급 업데이트 - 거래 ID: {}, 판매자: {} ({}), 구매자: {} ({})",
                transactionId,
                sellerUser.getNickname(),
                sellerUser.getLevelDisplayName(),
                buyerUser.getNickname(),
                buyerUser.getLevelDisplayName());
        // 🔥🔥🔥 등급 업데이트 끝 🔥🔥🔥

        try {
            notificationService.createTransactionCompleteNotification(updatedTransaction);
            log.info("거래 완료 알림 생성 완료 - 거래 ID: {}", transactionId);
        } catch (Exception e) {
            log.error("거래 완료 알림 생성 실패 - 거래 ID: {}, 오류: {}", transactionId, e.getMessage());
        }
    }

    // 🔥 구매 내역 조회 (기존 메서드 추가)
    public List<Transaction> getBuyerTransactions(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }

    // 🔥 판매 내역 조회 (기존 메서드 추가)
    public List<Transaction> getSellerTransactions(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
    }

    // 🔥 상품별 거래 내역 조회 (기존 메서드 추가)
    public List<Transaction> getProductTransactions(Long productId) {
        return transactionRepository.findByProduct_ProductId(productId);
    }
    // ✅ 특정 사용자의 구매 내역 조회
    public List<Transaction> getPurchasesByBuyer(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }
    // ✅ 판매자 기준 거래내역 조회
    public List<Transaction> getSalesBySeller(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
    }
    // ✅ 구매자 기준 거래 내역 조회
    public List<Transaction> findByBuyer(User buyer) {
        return transactionRepository.findByBuyer(buyer);
    }

    // ✅ 판매자 기준 거래 내역 조회
    public List<Transaction> findBySeller(User seller) {
        return transactionRepository.findBySeller(seller);
    }

    /**
     * 사용자의 거래 목록 조회 (카카오페이용 - userId로 조회)
     */
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactions(Long userId, String role) {
        if ("BUYER".equals(role)) {
            return transactionRepository.findByBuyer_UserIdOrderByCreatedDateDesc(userId);
        } else if ("SELLER".equals(role)) {
            return transactionRepository.findBySeller_UserIdOrderByCreatedDateDesc(userId);
        } else {
            throw new IllegalArgumentException("잘못된 역할입니다. BUYER 또는 SELLER만 가능합니다.");
        }
    }
}
