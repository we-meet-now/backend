package com.wemeetnow.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class StoreRecommendRequestDto {
    @JsonProperty("meetInfoMap")
    private MeetInfoMap meetInfoMap;

    @Data
    public static class MeetInfoMap {
        @JsonProperty("lat")
        private String lat; // 위도
        @JsonProperty("lng")
        private String lng; // 경도
        @JsonProperty("age")
        private int age; // 연령대
        @JsonProperty("meetType")
        private List<String> meetType; // 모임유형
        @JsonProperty("forbiddenFoodList")
        private List<String> forbiddenFoodList; // 못먹는음식유향
        @JsonProperty("memberCount")
        private int memberCount; // 모임인원
    }
}
