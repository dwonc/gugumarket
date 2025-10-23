package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 구매 내역 조회
    List<Transaction> findByBuyerOrderByTransactionDateDesc(User buyer);

    // 판매 내역 조회
    List<Transaction> findBySellerOrderByTransactionDateDesc(User seller);

    // 특정 상품의 거래 내역
    List<Transaction> findByProduct_ProductId(Long productId);
}