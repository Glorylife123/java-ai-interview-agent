package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tag {

    private Long id;
    private String name;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
