# 双令牌（Access Token + Refresh Token）接口测试

> 前置：先执行 `src/main/resources/db/refresh_tokens.sql` 建表。
> dev 环境已将 `app.jwt.cookie-secure=false`，因此可在本地 http 下用 curl 测试 Cookie。
> RT 通过 HttpOnly Cookie 下发，curl 用 `-c` 保存、`-b` 发送 Cookie。
>
> **鉴权范围（完整闭环）**：除 `/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`、
> `/api/auth/logout` 外，所有 `/api/**` 业务接口都需在请求头携带
> `Authorization: Bearer <access_token>`；`/api/admin/**` 还需该 AT 的角色为 ADMIN。
> AT 无效/过期返回 HTTP 401，非管理员访问管理端返回 HTTP 403。

## 0. 注册一个管理员账号

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<测试账号密码>","nickname":"管理员","role":"ADMIN"}'
```

## 1. 登录：签发 AT + RT（RT 写入 cookies.txt）

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Device-Id: device-abc" \
  -c cookies.txt \
  -d '{"username":"admin","password":"<测试账号密码>"}'
```

响应体：`{"code":0,"message":"success","data":{"access_token":"...","expires_in":900,"refresh_token":"..."}}`
响应头包含 `Set-Cookie: refresh_token=...; Max-Age=604800; Path=/; HttpOnly; SameSite=Strict`。

## 2. 刷新：轮换 RT，颁发新 AT（读旧 Cookie，写新 Cookie）

```bash
curl -s -X POST http://localhost:8080/api/auth/refresh \
  -b cookies.txt -c cookies.txt
```

响应体：`{"code":0,"message":"success","data":{"access_token":"...","expires_in":900}}`
数据库中旧 jti 的 `revoked_at` 被置为当前时间，新记录 `parent_jti` 指向旧 jti。

## 3. 宽限期验证（30 秒内的重传放行）

```bash
# 备份一份"旧 RT"（模拟客户端网络重传时仍持有旧 RT）
cp cookies.txt cookies_old.txt

# 用当前 RT 正常刷新一次（旧 RT 被置失效）
curl -s -X POST http://localhost:8080/api/auth/refresh -b cookies.txt -c cookies.txt

# 30 秒内：用刚失效的旧 RT 再刷新 —— 视为网络重传，依然成功（服务端打印 WARN 日志）
curl -s -X POST http://localhost:8080/api/auth/refresh -b cookies_old.txt

# 等待超过 30 秒后再用旧 RT —— 判定为复用，返回 401
sleep 31
curl -s -X POST http://localhost:8080/api/auth/refresh -b cookies_old.txt
# => {"code":401,"message":"Refresh Token 已失效","data":null}
```

## 4. 管理员踢人：吊销某用户全部有效 RT（需 ADMIN 的 AT）

```bash
# 取一枚有效 AT（从上面登录/刷新的响应体复制 access_token）
AT="<粘贴 access_token>"

# 假设踢掉 userId=1 的所有 RT
curl -s -X POST http://localhost:8080/api/admin/revoke/1 \
  -H "Authorization: Bearer $AT"

# 之后该用户再用其 RT 刷新会被拒绝
curl -s -X POST http://localhost:8080/api/auth/refresh -b cookies.txt
# => {"code":401,"message":"Refresh Token 已失效","data":null}
```

无 AT 或非 ADMIN 访问管理端：

```bash
curl -s -X POST http://localhost:8080/api/admin/revoke/1
# => {"code":401,"message":"缺少或非法的 Access Token","data":null}
```

## 5. 注销：失效当前设备 RT 并清除 Cookie

## 6. 访问受保护业务接口（需携带 AT）

```bash
AT="<粘贴 access_token>"

# 携带有效 AT：正常返回
curl -s "http://localhost:8080/api/questions?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $AT"

# 不带 AT：返回 HTTP 401
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8080/api/questions"
# => 401
```

