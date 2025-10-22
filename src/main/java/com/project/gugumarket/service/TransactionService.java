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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Controller에서 User를 미리 조회해서 넘기기
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

        return transactionRepository.save(transaction);
    }

    // 거래 조회
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다"));
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
}