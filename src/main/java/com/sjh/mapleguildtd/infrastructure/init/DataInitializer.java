package com.sjh.mapleguildtd.infrastructure.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final DataInitService dataInitService;

    @Override
    public void run(ApplicationArguments args) {
        if (dataInitService.isAlreadyInitialized()) {
            log.info("[DataInitializer] 초기 데이터가 이미 존재합니다. 건너뜁니다.");
            return;
        }
        log.info("[DataInitializer] 초기 데이터 등록 시작...");
        dataInitService.initialize();
        log.info("[DataInitializer] 초기 데이터 등록 완료.");
    }
}