package com.example.interviewagent.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    /**
     * 密码哈希。WRITE_ONLY：允许从请求体反序列化写入（如管理员建号），
     * 但绝不随任何响应序列化输出，避免 BCrypt 哈希经 GET/分页接口泄露。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
