package com.sjh.mapleguildtd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MapleGuildTdApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapleGuildTdApplication.class, args);
    }

}
