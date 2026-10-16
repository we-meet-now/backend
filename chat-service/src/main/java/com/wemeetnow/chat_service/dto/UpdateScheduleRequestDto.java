package com.wemeetnow.chat_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateScheduleRequestDto {
    private String title;
    @Schema(description = "시작 날짜(startDt), yyyyMMdd 형식", example = "20261016", requiredMode = Schema.RequiredMode.REQUIRED)
    private String startDt;
    @Schema(description = "시작 시각(startTim), HHmmss 형식", example = "125315", requiredMode = Schema.RequiredMode.REQUIRED)
    private String startTim;
    @Schema(description = "종료 날짜(endDt), yyyyMMdd 형식", example = "20261016", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endDt;
    @Schema(description = "종료 시각(endTim), HHmmss 형식", example = "135315", requiredMode = Schema.RequiredMode.REQUIRED)
    private String endTim;
    private String publicScheduleYn;
    private String publicMemberYn;
}
