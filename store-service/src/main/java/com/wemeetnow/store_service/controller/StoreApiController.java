package com.wemeetnow.store_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("")
public class StoreApiController {

    @GetMapping("/callback")
    private ResponseEntity callbackLogined(@RequestParam("code") String code) {
        // NOTE 카카오로부터 받은 code를 카카오에 토큰발급 요청하면 사용자 정보가 담겨져있는 토큰 받을 수 있다
        // 즉, https://kauth.kakao.com/oauth/token URL로 POST 요청을 보내면, 토큰을 받을 수 있다.
        Map<String, String> body = new HashMap();
        HttpStatus status = HttpStatus.OK;

        return ResponseEntity.status(status).body(body);
    }
}
