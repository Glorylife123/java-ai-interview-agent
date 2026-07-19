# Backend Demo Checklist: API, Redis, Benchmark

This folder is for the backend wrap-up before the Agent phase. It gives a repeatable demo path for Knife4j/Postman, Redis validation, and benchmark numbers that can be reused in resumes and interviews.

## 1. Start Services

Redis uses password `redis123` in local dev by default.

```powershell
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 ping
.\mvnw.cmd spring-boot:run
```

Open API docs:

- Knife4j: `http://localhost:8080/doc.html`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Postman collection:

- `docs/demo/postman/java-ai-interview-agent.postman_collection.json`

## 2. Redis Keys In This Phase

| Feature | Key | TTL |
| --- | --- | --- |
| Login access token cache | `login:token:{userId}` | Access Token TTL, default 15 min |
| Login failure rate limit | `interview:login:fail:{sha256(username + \0 + ip)}` | `app.redis.login-failure-window`, default 5 min |
| Question detail cache | `question:detail:{questionId}` | `question-detail-ttl` + jitter, default 15-20 min |
| Missing question null cache | `question:detail:{questionId}` with value `__NULL__` | `question-null-ttl`, default 1 min |
| Hot question list cache | `question:hot:list` | `hot-question-ttl`, default 5 min |
| Hot question rank | `question:hot:rank` | No TTL, ZSet cumulative rank |
| Practice overview stat cache | `user:practice:stat:{userId}` | `practice-stat-ttl`, default 5 min |
| Duplicate answer submit lock | `answer:submit:lock:{userId}:{questionId}` | `answer-submit-lock-ttl`, default 10s |

## 3. One-command Redis Benchmark

Run after the backend is started and a demo user/question exists:

```powershell
powershell -ExecutionPolicy Bypass -File docs/demo/scripts/redis-demo-benchmark.ps1 `
  -BaseUrl http://localhost:8080 `
  -RedisCli E:\Redis\redis-cli.exe `
  -RedisPassword redis123 `
  -Username demo_user `
  -Password 123456 `
  -QuestionId 1 `
  -Loops 30
```

Output:

- `docs/demo/results/redis-demo-metrics.md`

Optional update invalidation verification, if you have an admin account:

```powershell
powershell -ExecutionPolicy Bypass -File docs/demo/scripts/redis-demo-benchmark.ps1 `
  -Username demo_user -Password 123456 -QuestionId 1 -Loops 30 `
  -RunUpdateInvalidation -AdminUsername admin -AdminPassword 123456
```

## 4. Manual Redis Validation Commands

Replace `1` with a real question id.

### Question detail cache hit and TTL

```powershell
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 DEL question:detail:1
# Call GET /api/questions/1 once in Knife4j/Postman.
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 TTL question:detail:1
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 GET question:detail:1
# Call GET /api/questions/1 again. This is the cache-hit path.
```

Expected proof:

- First request creates `question:detail:1`.
- TTL is positive.
- Second request returns the same static detail from Redis, while dynamic counters are still refreshed from MySQL.

### Expiration

```powershell
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 EXPIRE question:detail:1 2
Start-Sleep -Seconds 3
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 EXISTS question:detail:1
# Call GET /api/questions/1 again; key should be recreated.
```

### Update invalidation

```text
1. Warm `question:detail:{questionId}` with GET /api/questions/{questionId}.
2. Confirm `EXISTS question:detail:{questionId}` returns 1.
3. As ADMIN, call PUT /api/questions/{questionId} with the current fields.
4. Confirm `EXISTS question:detail:{questionId}` returns 0.
```

### Hot question list cache

```powershell
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 DEL question:hot:list
# Call GET /api/questions/hot?limit=10 once.
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 TTL question:hot:list
# Call GET /api/questions/hot?limit=10 repeatedly. These are list-cache hits.
```

### Login failure limit

```text
Call POST /api/auth/login 6 times with the same username and a wrong password.
Expected: first 5 attempts return 401, the 6th returns 429.
```

### Duplicate answer submit lock

```text
Call POST /api/answer/submit twice quickly with the same userId and questionId.
Expected: first request succeeds, second request returns 429.
Confirm TTL with:
```

```powershell
E:\Redis\redis-cli.exe -h localhost -p 6379 -a redis123 TTL answer:submit:lock:{userId}:{questionId}
```

## 5. Resume-safe Metrics To Record

Use `docs/demo/results/redis-demo-metrics.md` as the source of truth.

| Metric | How to get it |
| --- | --- |
| Question detail no-cache vs cache-hit latency | Script compares repeated requests with `DEL question:detail:{id}` vs warmed cache |
| MySQL query count change | Code path: miss = 4 mapper calls; hit = 2 mapper calls; covered by `QuestionServiceImplTests` |
| Hot question cache hit rate | Script checks `EXISTS question:hot:list` before each hot-list request |
| Login failure limit effect | Script records statuses for 6 wrong-password attempts |
| Duplicate submit lock effect | Script records first/second submit status and lock TTL |