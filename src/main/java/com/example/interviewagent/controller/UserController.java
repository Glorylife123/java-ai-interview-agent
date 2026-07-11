package com.example.interviewagent.controller;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.common.Result;
import com.example.interviewagent.entity.User;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final UserService userService;

    @PostMapping
    public Result<User> create(@RequestBody User user,
                               @RequestAttribute("authRole") String operatorRole) {
        // 直接建号（可指定 role/status）属管理操作，仅 ADMIN 可用；普通注册走 /api/auth/register
        requireAdmin(operatorRole);
        return Result.success(userService.create(user));
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(required = false) Integer pageNum,
                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(userService.page(pageNum, pageSize));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user,
                               @RequestAttribute("authUserId") Long operatorId,
                               @RequestAttribute("authRole") String operatorRole) {
        boolean isAdmin = ROLE_ADMIN.equals(operatorRole);
        // 非管理员只能修改自己的账号
        if (!isAdmin && !operatorId.equals(id)) {
            throw new BusinessException(403, "无权操作他人账号");
        }
        // 非管理员禁止自改 role/status，防止提权或自行解禁
        if (!isAdmin) {
            user.setRole(null);
            user.setStatus(null);
        }
        return Result.success(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("authUserId") Long operatorId,
                               @RequestAttribute("authRole") String operatorRole) {
        if (!ROLE_ADMIN.equals(operatorRole) && !operatorId.equals(id)) {
            throw new BusinessException(403, "无权操作他人账号");
        }
        userService.delete(id);
        return Result.success();
    }

    private void requireAdmin(String operatorRole) {
        if (!ROLE_ADMIN.equals(operatorRole)) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }
}
