package com.example.interviewagent.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 刷新响应体：仅返回新的 Access Token 及其过期时间，新的 RT 通过 Cookie 下发。
 */
@Data
@AllArgsConstructor
public class RefreshResponse {

    @JsonProperty("access_token")
    private String accessToken;

    /** Access Token 有效期（秒）。 */
    @JsonProperty("expires_in")
    private long expiresIn;
}
