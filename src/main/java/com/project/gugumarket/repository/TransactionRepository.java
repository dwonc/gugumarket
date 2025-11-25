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

    // ========== 기존 메서드 (유지) ==========
    List<Transaction> findBySeller(User seller);
    List<Transaction> findByBuyer(User buyer);  // ⭐ 추가

    // ========== 카카오페이용 메서드 (새로 추가) ==========

    // 구매자의 거래 목록 (최신순)
    List<Transaction> findByBuyer_UserIdOrderByCreatedDateDesc(Long buyerId);

    // 판매자의 거래 목록 (최신순)
    List<Transaction> findBySeller_UserIdOrderByCreatedDateDesc(Long sellerId);

    // 특정 상품의 거래 목록
    List<Transaction> findByProduct_ProductIdOrderByCreatedDateDesc(Long productId);
}