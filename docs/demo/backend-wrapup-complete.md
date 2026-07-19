# Backend Wrap-up Complete Document

Generated: 2026-07-19
Purpose: close the backend Redis/demo phase and prepare for the next Agent development phase.

## 1. Current Backend Status

The backend is now in a demo-ready state for an internship/resume project presentation. It has:

- User registration/login with BCrypt password verification.
- Dual-token authentication: Access Token in response body, Refresh Token in HttpOnly Cookie.
- Refresh Token rotation, grace-period handling, replay detection, logout, and admin revocation.
- Question CRUD, pagination, type/tag filtering, view/submit/correct counters, and logical delete.
- Tag CRUD and question-tag binding.
- Answer submission and answer record paging.
- Wrong-question and practice-stat endpoints.
- Rule-based interview flow: create session, start, answer, score, generate report.
- Redis phase-one features that are small enough to explain in interviews.
- Knife4j / Swagger API documentation.
- Postman collection and Redis benchmark scripts.
- Frontend production build verified with Vite.

## 2. Redis Features Implemented

| Feature | Key | Behavior | TTL / Invalidation |
| --- | --- | --- | --- |
| Login token cache | `login:token:{userId}` | Stores latest Access Token after login/refresh | Access Token TTL, default 15 min; deleted on logout/revoke/replay detection |
| Login failure limit | `interview:login:fail:{hash}` | Lua `INCR + PEXPIRE`; blocks after configured failures | default 5 min; cleared after successful login |
| Question detail cache | `question:detail:{questionId}` | Cache Aside for static detail and tags | default 15 min + 0-5 min jitter; deleted on question/tag writes |
| Null detail cache | `question:detail:{questionId}` = `__NULL__` | Short cache for missing question | default 1 min |
| Hot question list | `question:hot:list` | Cached home-page hot question list | default 5 min; deleted on related question update/delete |
| Hot question rank | `question:hot:rank` | ZSet rank built from detail views | no TTL; cumulative score |
| Practice stat cache | `user:practice:stat:{userId}` | Cache overview stat | default 5 min; deleted after answer submit |
| Duplicate submit lock | `answer:submit:lock:{userId}:{questionId}` | Short lock for repeated answer submit | default 10s |

Design principle: Redis is a side-car optimization. Redis failures are logged and downgraded; core login/question/stat flows still rely on MySQL/JWT and continue working.

## 3. Demo Entry Points

| Demo Target | Path / Command |
| --- | --- |
| Knife4j | `http://localhost:8080/doc.html` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Postman collection | `docs/demo/postman/java-ai-interview-agent.postman_collection.json` |
| Redis validation guide | `docs/demo/redis-validation-guide.md` |
| Benchmark script | `docs/demo/scripts/redis-demo-benchmark.ps1` |
| Latest measurement report | `docs/demo/results/backend-measurement-report.md` |
| Raw Redis benchmark output | `docs/demo/results/redis-demo-metrics.md` |

## 4. Recommended Demo Flow

1. Start Redis and backend.

```powershell
E:\Redis\redis-cli.exe --no-auth-warning -h localhost -p 6379 -a redis123 ping
.\mvnw.cmd spring-boot:run
```

2. Open Knife4j at `/doc.html` and show all API groups are visible.

3. Use Postman collection:

- Register/login demo user.
- Get question page and pick a question id.
- Call question detail twice to demonstrate warm and hit path.
- Call hot questions to demonstrate `question:hot:list`.
- Call practice overview to demonstrate `user:practice:stat:{userId}`.
- Submit answer twice to demonstrate duplicate submit lock.
- Run wrong-password login 6 times to demonstrate login failure limit.

4. Use Redis CLI evidence from `docs/demo/redis-validation-guide.md`:

- `TTL question:detail:{id}`
- `EXISTS question:detail:{id}` after forced expiry and reload
- `TTL question:hot:list`
- `TTL user:practice:stat:{userId}`
- `TTL answer:submit:lock:{userId}:{questionId}`

5. Run benchmark script and show the generated report.

## 5. Measurement Results Captured

Latest complete measurement was taken after temporarily seeding 1000 demo questions. These seed questions were later logically deleted, and active question count returned to 12:

| Metric | Value |
| --- | --- |
| Active questions | 1012 |
| Detail no-cache avg | 38.39 ms |
| Detail cache-hit avg | 34.11 ms |
| Detail latency improvement | 11.15% |
| Detail p95 no-cache | 49.33 ms |
| Detail p95 cache-hit | 37.95 ms |
| Detail mapper calls | 4 -> 2 |
| Mapper call reduction | 50% |
| Hot question cache hit rate | 98% |
| Login limit | 401 x5, then 429 |
| Duplicate submit lock | first 200, second 429 |
| Update invalidation | admin no-op PUT deleted `question:detail:16` |

The detail latency number is intentionally conservative because the cache-hit path still updates view count and reads counters from MySQL. The better interview story is query reduction plus consistency preservation.

## 6. Files Added Or Updated In This Wrap-up

### Code

- `src/main/java/com/example/interviewagent/redis/AuthTokenRedisService.java`
- `src/main/java/com/example/interviewagent/redis/PracticeStatRedisService.java`
- `src/main/java/com/example/interviewagent/redis/AnswerSubmitLockService.java`
- `src/main/java/com/example/interviewagent/redis/QuestionRedisService.java`
- `src/main/java/com/example/interviewagent/service/impl/TokenServiceImpl.java`
- `src/main/java/com/example/interviewagent/service/impl/QuestionServiceImpl.java`
- `src/main/java/com/example/interviewagent/service/impl/AnswerRecordServiceImpl.java`
- `src/main/java/com/example/interviewagent/config/RedisFeatureProperties.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`

### Demo / Docs

- `README.md`
- `docs/demo/redis-validation-guide.md`
- `docs/demo/postman/java-ai-interview-agent.postman_collection.json`
- `docs/demo/scripts/redis-demo-benchmark.ps1`
- `docs/demo/results/redis-demo-metrics-template.md`
- `docs/demo/results/redis-demo-metrics.md`
- `docs/demo/results/api-smoke-metrics.json`
- `docs/demo/results/backend-measurement-report.md`

## 7. Verification Checklist

| Check | Status |
| --- | --- |
| Redis password config | fixed in dev profile: `${REDIS_PASSWORD:redis123}` |
| Redis PING | passed |
| OpenAPI JSON | passed, HTTP 200 |
| Knife4j page | passed, HTTP 200 |
| Postman JSON parse | passed |
| Benchmark script syntax | passed |
| Maven tests | passed: 26 tests, 0 failures |
| Frontend build | passed: `npm run build`; Vite chunk-size warning only |
| Redis TTL evidence | captured |
| Cache expiration evidence | captured |
| Cache update invalidation evidence | captured |
| Login failure limit evidence | captured |
| Duplicate submit lock evidence | captured |

## 8. Resume Project Bullets

Short version:

```text
Java AI Interview Agent：基于 Spring Boot + MyBatis + MySQL + Redis 的面试刷题与模拟面试系统，完成双 JWT 会话、题库/标签/答题/统计、规则版面试评分报告和 Redis 性能优化。接入 Cache Aside 题目详情缓存、热门题列表、练习统计缓存、登录失败限流和重复提交锁；在 1012 道题本地数据集上，详情缓存命中路径 mapper 调用由 4 次降至 2 次，热门题缓存命中率 98%。
```

Detailed version:

```text
- 设计并实现双令牌认证体系：Access Token + HttpOnly Refresh Token，支持 RT 轮换、宽限期、复用检测、同设备挤旧、注销和管理员吊销。
- 建设题库核心链路：题目/标签 CRUD、组合筛选、答题记录、练习统计、错题本和规则版模拟面试闭环。
- 接入 Redis 缓存与控制类能力：题目详情 Cache Aside、热门题列表缓存、练习统计缓存、登录失败限流、重复提交锁；Redis 异常时降级到 MySQL/JWT 主流程。
- 完成后端展示与验证材料：Knife4j/Postman 演示、Redis TTL/过期/失效验证、压测脚本和指标报告；本地 1012 题数据集下详情接口 mapper 调用减少 50%，热门题缓存命中率 98%。
```

## 9. Interview Talking Points

### Why is detail latency improvement not huge?

Because the hit path deliberately still writes `view_count` and reads `submit_count/correct_count/view_count` from MySQL. This keeps counters accurate. The optimization target is static detail and tags, not all SQL. The measurable result is mapper calls dropping from 4 to 2.

### Why not cache everything?

Caching dynamic counters would make the number prettier but introduces consistency and write-back complexity. For this phase, the safer design is Cache Aside for static detail plus MySQL for dynamic counters. A future optimization can move view count to Redis and flush asynchronously.

### Why use Redis for duplicate submit lock?

It gives a lightweight distributed lock across app instances. `SETNX + TTL` prevents short-time duplicate clicks without requiring database pessimistic locks.

### Why use Lua for login failure count?

`INCR + PEXPIRE` must be atomic. Lua avoids the race where a process increments a new key but fails before setting expiry.

## 10. Next Phase: Agent Development Entry

Backend is ready for Agent integration. Recommended next steps:

1. Define AI provider abstraction:
   - `ChatModelClient`
   - `AnswerEvaluationClient`
   - `InterviewQuestionGenerationClient`

2. Add prompt/version config:
   - prompt name
   - prompt version
   - model name
   - timeout
   - retry count

3. Replace rule-only scoring with strategy routing:
   - keep `RuleBasedAnswerEvaluator` as fallback
   - add `AiAnswerEvaluator`
   - record evaluator type in DB as `RULE` or `AI`

4. Add observability:
   - request id
   - latency
   - token usage or cost estimate
   - failure reason

5. Keep current demo path stable:
   - do not break Redis docs and benchmark script
   - preserve rule mode for offline demo when no API key is configured

## 11. Cleanup Notes

Local benchmark data inserted during measurement and cleanup status:

```sql
SELECT COUNT(*) FROM question WHERE source = 'REDIS_BENCH_SEED_20260719';
```

Cleanup already executed:

```sql
UPDATE question SET deleted = 1 WHERE source = 'REDIS_BENCH_SEED_20260719';
```

Cleanup verification: active seed questions = 0; total active questions = 12.

Temporary users created locally:

- `demo_20260719221616`
- `admin_demo_20260719225848`

These are local demo accounts and should not be used as production credentials.