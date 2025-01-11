package com.wemeetnow.auth_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {
    @PostMapping("/recommend-one")
    public ResponseEntity recommendOne(@RequestBody HashMap<String, Object> reqMap) {
//        [{'store_name': '샤브야', 'store_rating': '4.2', 'store_address_road': '경기 고양시 덕양구 충장로 18 2층', 'store_address_land': '행신동 761-6', 'store_sub_category': '샤브샤브'}
//, {'store_name': '향원', 'store_rating': '3.2', 'store_address_road': '경기 고양시 덕양구 무원로36번길 15-2 1층', 'store_address_land': '행신동 722-12', 'store_sub_category': '중국요리'}
//, {'store_name': '선식당 행신본점', 'store_rating': '3.6', 'store_address_road': '경기 고양시 덕양구 충장로103번길 27 1층', 'store_address_land': '행신동 633-23', 'store_sub_category': '양식'}
//, {'store_name': '백두산참숯화로구 이', 'store_rating': '3.4', 'store_address_road': '경기 고양시 덕양구 충장로 7 별제회관 2층 201~203호', 'store_address_land': '행신동 707-2', 'store_sub_category': '육류,고기'}
//, {'store_name': '소담촌 행신점', 'store_rating': '4.3', 'store_address_road': '경기 고양시 덕양구 충장로 2 센트럴빌딩 2층', 'store_address_land': '행신동 762-5', 'store_sub_category': '샤브샤브'}]
        for (String key : reqMap.keySet()) {
            log.info("reqMap.get({}): {}", key, reqMap.get(key));
        }
        HttpStatus status = HttpStatus.OK;
        String statusCd = "2000";
        Map<String, Object> body = new HashMap<>();
        try {
            Map<String, Object> resMap = new HashMap<>();
            Map<String, Object> resStore = new HashMap<>();
            resStore.put("store_name", "샤브야");
            resStore.put("store_rating", "4.2");
            resStore.put("store_address_road", "경기 고양시 덕양구 충장로 18 2층");
            resStore.put("store_address_land", "행신동 761-6");
            resStore.put("store_sub_category", "샤브샤브");
            resStore.put("recommend_reason", "샤브야는 샤브샤브 전문점으로 고객 평가가 높고, 식당 분위기와 음식 퀄리티가 좋아 친구들과 모임하기에 적합합니다. 또한, 샤브야 메뉴 중에서도 강한 향신료나 매운 음식을 피할 수 있는 옵션이 다양하게 제공되어 25대 연령의 고객들에게 즐거운 식사를 제공할 것으로 기대됩니다.");
            resMap.put("restaurant", resStore);
            statusCd = "2000";
            body.put("data", resMap);
        } catch (Exception e) {
            log.error("raised error: ", e.getMessage());
            statusCd = "4000";
        } finally {
            body.put("statusCd", statusCd);
        }

//        {
//    "restaurants": {
//		    "store_name": "샤브야",
//        "store_rating": "4.2",
//        "store_address_road": "경기 고양시 덕양구 충장로 18 2층",
//        "store_address_land": "행신동 761-6",
//        "store_sub_category": "샤브샤브"
//    },
//    "recommend_reason": "샤브야는 샤브샤브 전문점으로 고객 평가가 높고, 식당 분위기와 음식 퀄리티가 좋아 친구들과 모임하기에 적합합니다. 또한, 샤브야 메뉴 중에서도 강한 향신료나 매운 음식을 피할 수 있는 옵션이 다양하게 제공되어 25대 연령의 고객들에게 즐거운 식사를 제공할 것으로 기대됩니다."
//  }
        return ResponseEntity.status(status).body(body);
    }
}
