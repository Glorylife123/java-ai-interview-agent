package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端接口。/api/admin/** 由 AdminAuthInterceptor 校验 Access Token 且要求 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TokenService tokenService;

    /**
     * 管理员踢人：吊销指定用户全部仍有效的 RT。
     * 被踢用户现有 AT 在其 15 分钟有效期内仍可用，到期后因无法刷新而彻底登出。
     */
    @PostMapping("/revoke/{userId}")
    public Result<Void> revoke(@PathVariable Long userId) {
        tokenService.revokeAllForUser(userId);
        return Result.success();
    }
}
