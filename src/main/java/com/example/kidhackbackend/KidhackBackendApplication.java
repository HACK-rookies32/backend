package com.example.kidhackbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidhackBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidhackBackendApplication.class, args);
    }

}
