package com.project.gugumarket.service;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오페이 결제 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    @Value("${kakaopay.admin-key}")
    private String adminKey;

    @Value("${kakaopay.cid}")
    private String cid;

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String READY_URL = "https://open-api.kakaopay.com/online/v1/payment/ready";
    private static final String APPROVE_URL = "https://open-api.kakaopay.com/online/v1/payment/approve";

    /**
     * 카카오페이 결제 준비
     */
    @Transactional
    public KakaoPayReadyResponse kakaoPayReady(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        if (!transaction.getBuyer().getUserId().equals(userId)) {
            throw new IllegalArgumentException("구매자만 결제할 수 있습니다.");
        }

        Product product = transaction.getProduct();

        // 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("partner_order_id", transactionId.toString());
        params.add("partner_user_id", userId.toString());
        params.add("item_name", product.getTitle());
        params.add("quantity", "1");
        params.add("total_amount", product.getPrice().toString());
        params.add("tax_free_amount", "0");
        params.add("approval_url", "http://localhost:8080/api/payment/kakaopay/success?transaction_id=" + transactionId);
        params.add("cancel_url", "http://localhost:8080/api/payment/kakaopay/cancel?transaction_id=" + transactionId);
        params.add("fail_url", "http://localhost:8080/api/payment/kakaopay/fail?transaction_id=" + transactionId);

        // ⭐ 헤더 수정 (SECRET_KEY + application/x-www-form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "DEV_SECRET_KEY " + adminKey);  // ⭐ DEV_ 제거!
        headers.add("Content-Type", "application/x-www-form-urlencoded");  // ⭐ 변경!

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            KakaoPayReadyResponse response = restTemplate.postForObject(
                    READY_URL,
                    requestEntity,
                    KakaoPayReadyResponse.class
            );

            if (response == null || response.getTid() == null) {
                throw new RuntimeException("카카오페이 결제 준비 응답이 올바르지 않습니다.");
            }

            transactionService.updateKakaoPayTid(transactionId, response.getTid());

            log.info("카카오페이 결제 준비 성공 - transactionId: {}, tid: {}", transactionId, response.getTid());
            return response;

        } catch (Exception e) {
            log.error("카카오페이 결제 준비 실패", e);
            throw new RuntimeException("카카오페이 결제 준비 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 카카오페이 결제 승인
     */
    @Transactional
    public KakaoPayApproveResponse kakaoPayApprove(Long transactionId, String pgToken) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        String tid = transaction.getTid();
        if (tid == null || tid.isEmpty()) {
            throw new IllegalStateException("결제 준비가 완료되지 않았습니다.");
        }

        // 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("cid", cid);
        params.add("tid", tid);
        params.add("partner_order_id", transactionId.toString());
        params.add("partner_user_id", transaction.getBuyer().getUserId().toString());
        params.add("pg_token", pgToken);

        // ⭐ 헤더 수정 (SECRET_KEY + application/x-www-form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "DEV_SECRET_KEY " + adminKey);  // ⭐ DEV_ 제거!
        headers.add("Content-Type", "application/x-www-form-urlencoded");  // ⭐ 변경!

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

        try {
            KakaoPayApproveResponse response = restTemplate.postForObject(
                    APPROVE_URL,
                    requestEntity,
                    KakaoPayApproveResponse.class
            );

            if (response == null || response.getAid() == null) {
                throw new RuntimeException("카카오페이 결제 승인 응답이 올바르지 않습니다.");
            }

            transactionService.completeKakaoPayment(
                    transactionId,
                    response.getAid(),
                    response.getPaymentMethodType()
            );

            log.info("카카오페이 결제 승인 성공 - transactionId: {}, aid: {}", transactionId, response.getAid());
            return response;

        } catch (Exception e) {
            log.error("카카오페이 결제 승인 실패", e);
            throw new RuntimeException("카카오페이 결제 승인 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}