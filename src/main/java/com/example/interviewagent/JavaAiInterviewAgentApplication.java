package com.example.interviewagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.interviewagent.mapper")
@SpringBootApplication
public class JavaAiInterviewAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaAiInterviewAgentApplication.class, args);
    }
}
