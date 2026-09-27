package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.domain.EnterCodePool;
import com.wemeetnow.chat_service.domain.EnterCodeMgmt;
import com.wemeetnow.chat_service.repository.EnterCodeMgmtRepository;
import com.wemeetnow.chat_service.repository.EnterCodePoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EnterCodeGenerator {
    private final EnterCodeMgmtRepository enterCodeMgmtRepository;
    private final EnterCodePoolRepository enterCodePoolRepository;
    private static final int RANDOM_CODE_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMdd");

    /**
     * 입장코드 생성 (현재 날짜의 mmdd + 랜덤 4자리)
     * 
     * @param chatRoomId 채팅방 ID
     * @return 생성된 enterCode (예: 09271234)
     */
    public String generateEnterCode(Long chatRoomId) {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DATE_FORMATTER);
        
        String randomCode = generateUniqueRandomCode(today);
        String enterCode = datePrefix + randomCode;
        
        // EnterCodePool에 저장
        saveEnterCodeToPool(today, randomCode);
        
        // EnterCodeMgmt에 저장 (chatRoomId와 enterCode)
        saveEnterCodeToMgmt(chatRoomId, enterCode);
        
        log.info("Generated enterCode: {} for chatRoomId: {}", enterCode, chatRoomId);
        return enterCode;
    }

    /**
     * 중복되지 않는 랜덤 4자리 숫자 생성
     * 
     * @param codeDate 오늘 날짜
     * @return 4자리 랜덤 코드
     */
    private String generateUniqueRandomCode(LocalDate codeDate) {
        // 오늘 날짜에 이미 생성된 코드들 조회
        List<EnterCodePool> usedCodes = enterCodePoolRepository.findUsedCodesByDate(codeDate);
        Set<String> usedCodeSet = usedCodes.stream()
                .map(EnterCodePool::getRandomCode)
                .collect(Collectors.toSet());

        Random random = new Random();
        String randomCode;
        int attempts = 0;

        do {
            randomCode = String.format("%04d", random.nextInt(10000));
            attempts++;
            if (attempts > MAX_ATTEMPTS) {
                throw new IllegalStateException("Failed to generate unique random code after " + MAX_ATTEMPTS + " attempts");
            }
        } while (usedCodeSet.contains(randomCode));

        return randomCode;
    }

    /**
     * EnterCodePool 테이블에 저장
     * 
     * @param codeDate 날짜
     * @param randomCode 4자리 랜덤 코드
     */
    private void saveEnterCodeToPool(LocalDate codeDate, String randomCode) {
        EnterCodePool enterCodePool = EnterCodePool.builder()
                .codeDate(codeDate)
                .randomCode(randomCode)
                .useYn('Y')
                .build();
        enterCodePoolRepository.save(enterCodePool);
        log.debug("Saved to EnterCodePool - date: {}, randomCode: {}", codeDate, randomCode);
    }

    /**
     * EnterCodeMgmt 테이블에 저장
     * 
     * @param chatRoomId 채팅방 ID
     * @param enterCode 전체 입장코드 (mmdd + 4자리)
     */
    private void saveEnterCodeToMgmt(Long chatRoomId, String enterCode) {
        EnterCodeMgmt enterCodeMgmt = EnterCodeMgmt.builder()
                .chatRoomId(chatRoomId)
                .enterCode(enterCode)
                .build();
        enterCodeMgmtRepository.save(enterCodeMgmt);
        log.debug("Saved to EnterCodeMgmt - chatRoomId: {}, enterCode: {}", chatRoomId, enterCode);
    }
}
