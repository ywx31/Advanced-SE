package com.mathmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.mathmate.mapper")
public class MathMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(MathMateApplication.class, args);
    }
}
