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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j  // 🔥 로깅 추가
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
<<<<<<< HEAD

    // Controller에서 User를 미리 조회해서 넘기기
=======
    private final NotificationService notificationService;  // 🔥 알림 서비스 추가

    // Controller에서 User를 미리 조회해서 넘기기
    @Transactional  // 🔥 트랜잭션 추가
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
    public Transaction createTransaction(Long productId, User buyer, PurchaseDto dto) {
        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));

        // 판매자 본인이 구매하는 경우 방지
        if (product.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("본인의 상품은 구매할 수 없습니다");
        }

        // 이미 판매된 상품인지 확인
        if (product.getStatus() == ProductStatus.SOLD_OUT) {
            throw new IllegalArgumentException("이미 판매완료된 상품입니다");
        }

        // 거래 생성
        Transaction transaction = Transaction.builder()
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .depositorName(dto.getDepositorName())
                .status(TransactionStatus.PENDING)
                .transactionDate(LocalDateTime.now())
                .build();

        // 상품 상태를 예약중으로 변경
        product.updateStatus(ProductStatus.RESERVED);
        productRepository.save(product);

<<<<<<< HEAD
        return transactionRepository.save(transaction);
=======
        Transaction saved = transactionRepository.save(transaction);

        // 🔥 구매 알림 생성
        try {
            notificationService.createPurchaseNotification(saved);
            log.info("구매 알림 생성 완료 - 거래 ID: {}, 구매자: {}, 판매자: {}",
                    saved.getTransactionId(),
                    buyer.getNickname(),
                    product.getSeller().getNickname());
        } catch (Exception e) {
            log.error("구매 알림 생성 실패 - 거래 ID: {}, 오류: {}",
                    saved.getTransactionId(), e.getMessage());
            // 알림 생성 실패해도 거래는 정상 처리
        }

        return saved;
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
    }

    // 거래 조회
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));
<<<<<<< HEAD
    }

    // 입금자명 수정
    public void updateDepositor(Long transactionId, String depositorName) {
        Transaction transaction = getTransaction(transactionId);
        transaction.updateDepositor(depositorName);
        transactionRepository.save(transaction);
    }

    // 거래 취소
    public void cancelTransaction(Long transactionId, String username) {
        Transaction transaction = getTransaction(transactionId);

        if (!transaction.getBuyer().getUserName().equals(username)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        transaction.cancel();
        transactionRepository.save(transaction);

        // 상품 상태 원복
        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SALE);
        productRepository.save(product);
    }
    // 특정 사용자의 구매 내역 조회
    public List<Transaction> getPurchasesByBuyer(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
=======
    }

    // 입금자명 수정
    @Transactional
    public void updateDepositor(Long transactionId, String depositorName) {
        Transaction transaction = getTransaction(transactionId);
        transaction.updateDepositor(depositorName);
        transactionRepository.save(transaction);

        log.info("입금자명 수정 완료 - 거래 ID: {}, 입금자명: {}", transactionId, depositorName);
    }

    // 거래 취소
    @Transactional
    public void cancelTransaction(Long transactionId, String username) {
        Transaction transaction = getTransaction(transactionId);

        if (!transaction.getBuyer().getUserName().equals(username)) {
            throw new IllegalArgumentException("권한이 없습니다");
        }

        transaction.cancel();
        transactionRepository.save(transaction);

        // 상품 상태 원복
        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SALE);
        productRepository.save(product);

        log.info("거래 취소 완료 - 거래 ID: {}, 사용자: {}", transactionId, username);
    }

    // 🔥 거래 완료 메서드 추가 (판매자가 거래 완료 처리)
    @Transactional
    public void completeTransaction(Long transactionId, User seller) {
        Transaction transaction = getTransaction(transactionId);

        // 판매자 본인인지 확인
        if (!transaction.getSeller().getUserId().equals(seller.getUserId())) {
            throw new IllegalArgumentException("판매자만 거래를 완료할 수 있습니다");
        }

        // 거래 상태가 PENDING인지 확인
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalArgumentException("완료할 수 없는 거래 상태입니다");
        }

        // 거래 완료 처리
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

        // 상품 상태를 판매완료로 변경
        Product product = transaction.getProduct();
        product.updateStatus(ProductStatus.SOLD_OUT);
        productRepository.save(product);

        log.info("거래 완료 처리 - 거래 ID: {}, 판매자: {}", transactionId, seller.getNickname());

        // 🔥 거래 완료 알림 생성
        try {
            notificationService.createTransactionCompleteNotification(updatedTransaction);
            log.info("거래 완료 알림 생성 완료 - 거래 ID: {}", transactionId);
        } catch (Exception e) {
            log.error("거래 완료 알림 생성 실패 - 거래 ID: {}, 오류: {}", transactionId, e.getMessage());
            // 알림 생성 실패해도 거래 완료는 정상 처리
        }
    }

    // 🔥 구매 내역 조회 (기존 메서드 추가)
    public List<Transaction> getBuyerTransactions(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }

    // 🔥 판매 내역 조회 (기존 메서드 추가)
    public List<Transaction> getSellerTransactions(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
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
}