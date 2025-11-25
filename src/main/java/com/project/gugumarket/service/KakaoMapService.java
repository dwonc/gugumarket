package com.project.gugumarket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오 지도 API를 사용한 주소 → 좌표 변환 서비스
 */
@Service
@Slf4j
public class KakaoMapService {

    @Value("${kakao.api.key:d5c8e66d1c468fb8de8e17433a8bc6f2}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 주소를 위도/경도로 변환
     * @param address 주소 문자열
     * @return Map with "latitude" and "longitude"
     */
    public Map<String, Double> getCoordinatesFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.warn("주소가 비어있습니다");
            return null;
        }

        try {
            String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" + address;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonObject = new JSONObject(response.getBody());
                JSONArray documents = jsonObject.getJSONArray("documents");

                if (documents.length() > 0) {
                    JSONObject document = documents.getJSONObject(0);

                    double latitude = document.getDouble("y");  // 위도
                    double longitude = document.getDouble("x"); // 경도

                    Map<String, Double> coordinates = new HashMap<>();
                    coordinates.put("latitude", latitude);
                    coordinates.put("longitude", longitude);

                    log.info("✅ 주소 변환 성공: {} -> ({}, {})", address, latitude, longitude);
                    return coordinates;
                }
            }

            log.warn("⚠️ 주소 변환 실패: {}", address);
            return null;

        } catch (Exception e) {
            log.error("❌ Kakao API 호출 실패: {}", e.getMessage());
            return null;
        }
    }
}