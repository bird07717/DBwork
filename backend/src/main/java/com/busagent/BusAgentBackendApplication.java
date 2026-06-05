package com.busagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.busagent.mapper")
@SpringBootApplication
public class BusAgentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusAgentBackendApplication.class, args);
    }
}
