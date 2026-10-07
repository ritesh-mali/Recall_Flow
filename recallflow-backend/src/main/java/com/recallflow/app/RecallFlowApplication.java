package com.recallflow.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecallFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecallFlowApplication.class, args);
    }
}
