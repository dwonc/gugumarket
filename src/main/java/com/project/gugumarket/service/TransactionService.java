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
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Transaction createTransaction(Long productId, User buyer, PurchaseDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));

        if (product.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("본인의 상품은 구매할 수 없습니다");
        }

        if (product.getStatus() == ProductStatus.SOLD_OUT) {
            throw new IllegalArgumentException("이미 판매완료된 상품입니다");
        }

        Transaction transaction = Transaction.builder()
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .depositorName(dto.getDepositorName())
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

    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));
    }

    @Transactional
    public void updateDepositor(Long transactionId, String depositorName) {
        Transaction transaction = getTransaction(transactionId);
        transaction.updateDepositor(depositorName);
        transactionRepository.save(transaction);

        log.info("입금자명 수정 완료 - 거래 ID: {}, 입금자명: {}", transactionId, depositorName);
    }

    @Transactional
    public void cancelTransaction(Long transactionId, String username) {
        Transaction transaction = getTransaction(transactionId);

        if (!transaction.getBuyer().getUserName().equals(username)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        transaction.cancel();
        transactionRepository.save(transaction);

        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SALE);
        productRepository.save(product);

        log.info("거래 취소 완료 - 거래 ID: {}, 사용자: {}", transactionId, username);
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

    public List<Transaction> getBuyerTransactions(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }

    public List<Transaction> getSellerTransactions(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
    }

    public List<Transaction> getProductTransactions(Long productId) {
        return transactionRepository.findByProduct_ProductId(productId);
    }

    public List<Transaction> getPurchasesByBuyer(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }

    public List<Transaction> getSalesBySeller(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
    }

    public List<Transaction> findByBuyer(User buyer) {
        return transactionRepository.findByBuyer(buyer);
    }

    public List<Transaction> findBySeller(User seller) {
        return transactionRepository.findBySeller(seller);
    }
}