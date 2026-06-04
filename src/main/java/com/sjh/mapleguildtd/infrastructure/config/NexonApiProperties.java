package com.sjh.mapleguildtd.infrastructure.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "nexon.api")
public class NexonApiProperties {

    private final String baseUrl;
    private final int cacheTtlHours;
}
