package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.StoreRecommendRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "매장 API", description = "매장 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {
    @Operation(summary = "매장 1개 추천", description = "추천된 5개의 매장 중 1개를 Map<>형태로 추천해준다.")
    @Parameter(name = "Map", description = "meetInfoMap")
    @CrossOrigin(origins = "https://localhost:3000")
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

        return ResponseEntity.status(status).body(body);
    }
    @Operation(summary = "매장 3개 추천", description = "추천된 3개의 매장을 Map<String, List<String>>형태로 추천해준다.")
    @Parameter(name = "Map", description = "meetInfoMap")
    @CrossOrigin(origins = "https://localhost:3000")
    @PostMapping("/recommend-three")
    public ResponseEntity recommendThree(@RequestBody StoreRecommendRequestDto storeRecommendReqDto) {

        String statusCd = "2000";
        HttpStatus status = HttpStatus.OK;
        Map<String, Object> bodyMap = new HashMap<>();
        List<Map<String, Object>> bodyList = new ArrayList<>();
        try {
            Map<String, Object> resStore1 = new HashMap<>();
            resStore1.put("store_name", "샤브야");
            resStore1.put("store_rating", "4.2");
            resStore1.put("store_address_road", "경기 고양시 덕양구 충장로 18 2층");
            resStore1.put("store_address_land", "행신동 761-6");
            resStore1.put("store_sub_category", "샤브샤브");
            resStore1.put("recommend_reason", "샤브야는 샤브샤브 전문점으로 고객 평가가 높고, 식당 분위기와 음식 퀄리티가 좋아 친구들과 모임하기에 적합합니다. 또한, 샤브야 메뉴 중에서도 강한 향신료나 매운 음식을 피할 수 있는 옵션이 다양하게 제공되어 25대 연령의 고객들에게 즐거운 식사를 제공할 것으로 기대됩니다.");
            resStore1.put("thumbnail_url", "http://t1.daumcdn.net/place/3F10BCE7BEB94F37A28C67AA9CECD7C7");
            resStore1.put("open_status", "영업중");
            resStore1.put("open_time", " 11:00 ~ 22:00");
            resStore1.put("tel_num", "031-971-9979");
            resStore1.put("review_cnt", "71");

            Map<String, Object> resStore2 = new HashMap<>();
            resStore2.put("store_name", "소담촌 행신점");
            resStore2.put("store_rating", "4.2");
            resStore2.put("store_address_road", "경기 고양시 덕양구 충장로 2 센트럴빌딩 2층");
            resStore2.put("store_address_land", "행신동 762-5");
            resStore2.put("store_sub_category", "샤브샤브");
            resStore2.put("recommend_reason", "샤브샤브 전문점으로 다양한 재료를 선택하여 담백하고 부담 없이 즐길 수 있습니다. 또한 조용한 분위기의 공간으로 친구들과 편안하게 대화하며 식사하기 좋습니다.");
            resStore2.put("thumbnail_url", "http://t1.kakaocdn.net/mystore/69DC26CA0B2B4EF8A91D8BFFA1EBF25D");
            resStore2.put("open_status", "브레이크타임");
            resStore2.put("open_time", " 15:00 ~ 17:00");
            resStore2.put("tel_num", "031-979-9900");
            resStore2.put("review_cnt", "31");

            Map<String, Object> resStore3 = new HashMap<>();
            resStore3.put("store_name", "선식당 행신본점");
            resStore3.put("store_rating", "3.6");
            resStore3.put("store_address_road", "경기 고양시 덕양구 충장로103번길 27 1층");
            resStore3.put("store_address_land", "행신동 633-23");
            resStore3.put("store_sub_category", "양식");
            resStore3.put("recommend_reason", "양식 메뉴가 주를 이루어 강한 향신료나 매운 음식을 피하면서도 맛있는 식사를 할 수 있습니다. 깔끔한 분위기로 친구들과 모임을 갖기에 적합한 장소입니다.");
            resStore3.put("thumbnail_url", "http://t1.daumcdn.net/place/B1E0D29D4865414CAE0EC1AED35678F4");
            resStore3.put("open_status", "휴무일");
            resStore3.put("open_time", "휴무일 일요일");
            resStore3.put("tel_num", "02-511-6247");
            resStore3.put("review_cnt", "311");
            bodyList.add(resStore1);
            bodyList.add(resStore2);
            bodyList.add(resStore3);

            bodyMap.put("data", bodyList);
        } catch (Exception e) {
            log.error("raised error: ", e.getMessage());
            statusCd = "4000";
        } finally {
            bodyMap.put("statusCd", statusCd);
        }
        return ResponseEntity.status(status).body(bodyMap);
    }
}
