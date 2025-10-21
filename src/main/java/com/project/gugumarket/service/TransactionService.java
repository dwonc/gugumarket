package com.project.gugumarket.service;

import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductService productService;

    /**
     * 거래 완료 처리
     */
    @Transactional
    public Transaction completeTransaction(Long productId, User seller, User buyer) {
        // 상품 조회
        Product product = productService.getProduct(productId);

        // 판매자 권한 확인
        if (!product.getSeller().equals(seller)) {
            throw new IllegalArgumentException("판매자만 거래 완료 처리를 할 수 있습니다.");
        }

        // 이미 판매된 상품인지 확인
        if ("SOLD_OUT".equals(product.getStatus())) {
            throw new IllegalStateException("이미 판매 완료된 상품입니다.");
        }

        // 거래 내역 생성
        Transaction transaction = Transaction.builder()
                .product(product)
                .seller(seller)
                .buyer(buyer)
                .status("COMPLETED")
                .build();

        // 상품 상태를 판매완료로 변경
        product.setStatus(ProductStatus.SOLD_OUT);
        productService.save(product);

        // 거래 내역 저장
        return transactionRepository.save(transaction);
    }

    /**
     * 구매 내역 조회
     */
    public List<Transaction> getPurchaseHistory(User buyer) {
        return transactionRepository.findByBuyerOrderByTransactionDateDesc(buyer);
    }

    /**
     * 판매 내역 조회
     */
    public List<Transaction> getSalesHistory(User seller) {
        return transactionRepository.findBySellerOrderByTransactionDateDesc(seller);
    }
}