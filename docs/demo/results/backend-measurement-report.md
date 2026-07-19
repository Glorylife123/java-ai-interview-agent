# Backend Measurement Report

Generated: 2026-07-19 23:12 Asia/Shanghai
Branch during measurement: `feat/backend-demo-redis-wrapup`
Backend: `http://localhost:8080`
Redis: `localhost:6379`, password `redis123`
Database: local MySQL `interview_agent`
Dataset during measurement: 1012 active questions, including 1000 benchmark seed questions with `source = REDIS_BENCH_SEED_20260719`. Cleanup after measurement: 1000 seed questions were logically deleted; active questions returned to 12.
Benchmark question: `questionId = 16`
Benchmark user: `demo_20260719221616`, `userId = 4`
Admin validation user: `admin_demo_20260719225848`

## 1. Summary

| Area | Result |
| --- | --- |
| Backend unit/integration tests | `mvnw.cmd test`: 26 tests, 0 failures, 0 errors |
| Frontend production build | `npm run build`: passed, Vite built in 7.60s; chunk-size warning only |
| Redis connectivity | `PING -> PONG` |
| OpenAPI JSON | HTTP 200, 1562.75 ms |
| Knife4j UI | HTTP 200, 82.95 ms |
| Postman collection JSON | valid JSON |
| Redis benchmark script syntax | valid PowerShell script |
| Question detail cache | hit path latency reduced by 11.15%; mapper calls reduced by 50% |
| Hot question cache | 98% hit rate in 50-request loop |
| Login failure limit | 5 wrong attempts return 401, 6th returns 429 |
| Duplicate submit lock | first submit 200, immediate duplicate 429 |
| Cache expiration | forced 2s expiry removed detail cache, next request recreated it |
| Update invalidation | no-op admin PUT returned 200 and deleted `question:detail:16` |

## 2. API Smoke Measurements

| Endpoint / Check | Status | Time |
| --- | ---: | ---: |
| `GET /v3/api-docs` | 200 | 1562.75 ms |
| `GET /doc.html` | 200 | 82.95 ms |
| `GET /api/questions?pageNum=1&pageSize=20` | 200 | 37.23 ms |
| `GET /api/questions?keyword=Redis&pageNum=1&pageSize=20` | 200 | 24.23 ms |
| `GET /api/questions?questionType=八股题&pageNum=1&pageSize=20` | 200 | 62.06 ms |
| `GET /api/questions/hot?limit=10` | 200 | 14.74 ms |
| `GET /api/stat/overview` | 200 | 13.03 ms |

Raw file: `docs/demo/results/api-smoke-metrics.json`

## 3. Redis Benchmark Metrics

Command:

```powershell
powershell -ExecutionPolicy Bypass -File docs/demo/scripts/redis-demo-benchmark.ps1 `
  -BaseUrl http://localhost:8080 `
  -RedisCli E:\Redis\redis-cli.exe `
  -RedisPassword redis123 `
  -Username demo_20260719221616 `
  -Password 123456 `
  -QuestionId 16 `
  -Loops 50 `
  -LimitUsername redis_limit_demo_202607192304 `
  -RunUpdateInvalidation `
  -AdminUsername admin_demo_20260719225848 `
  -AdminPassword 123456
```

| Metric | Result |
| --- | --- |
| Question detail no-cache avg | 38.39 ms |
| Question detail cache-hit avg | 34.11 ms |
| Question detail latency improvement | 11.15% |
| Question detail p95 no-cache | 49.33 ms |
| Question detail p95 cache-hit | 37.95 ms |
| MySQL query count, no-cache path | 4 mapper calls: `select question`, `select tags`, `update view_count`, `select counters` |
| MySQL query count, cache-hit path | 2 mapper calls: `update view_count`, `select counters` |
| MySQL query reduction | 50% on detail cache hit path |
| Hot question cache hit rate | 98%: 49 hits / 50 requests |
| Login failure limit effect | `401, 401, 401, 401, 401, 429` |
| Duplicate submit lock effect | first submit 200, second submit 429, lock TTL 10s |

Raw file: `docs/demo/results/redis-demo-metrics.md`

## 4. Redis Evidence

| Key | Evidence |
| --- | --- |
| `question:detail:16` | TTL after warm = 1193s; forced 2s expiry removed key; reload recreated key |
| `question:hot:list` | TTL after hot query = 296s |
| `user:practice:stat:4` | stat query set TTL = 300s; answer submit deleted the cache |
| `answer:submit:lock:4:16` | first submit created lock with TTL = 10s; immediate duplicate returned 429 |
| login failure key | IPv6 loopback key TTL = 300s after repeated wrong-password attempts |
| update invalidation | admin no-op `PUT /api/questions/16` returned 200; detail key changed from exists=1 to exists=0 |

## 5. Interpretation For Interview

The detail endpoint latency improvement is moderate because the cache-hit path intentionally still writes `view_count` and reads dynamic counters from MySQL. The stronger engineering point is the query reduction and consistency tradeoff:

- Cache miss path: load static question detail and tags from MySQL, update view count, then read dynamic counters.
- Cache hit path: reuse static detail from Redis, only update view count and read dynamic counters from MySQL.
- Result: mapper calls reduced from 4 to 2 while keeping frequently changing counters accurate.

This is a defensible design choice for an interview: the system does not over-cache dynamic counters just to chase better benchmark numbers.

## 6. Resume-ready Wording

```text
为 Java 面试题库系统接入 Redis Cache Aside 缓存、热门题列表缓存、练习统计缓存、登录失败限流和重复提交锁；在 1012 道题本地数据集上，题目详情缓存命中路径 mapper 调用由 4 次降至 2 次，平均耗时由 38.39 ms 降至 34.11 ms，热门题缓存命中率 98%，并完成 TTL、过期重建、更新失效、登录限流和重复提交锁验证。
```

## 7. Cleanup After Measurement

| Item | Result |
| --- | --- |
| Seed questions | 1000 rows with `source = REDIS_BENCH_SEED_20260719` were logically deleted via `deleted = 1` |
| Active seed questions after cleanup | 0 |
| Total active questions after cleanup | 12 |
| Redis cleanup | Removed `question:detail:16`, `question:hot:list`, and ZSet member `16` from `question:hot:rank` |

## 8. Known Limits

- This is a local single-machine measurement, not a distributed production benchmark.
- Detail endpoint optimization is capped because `view_count` and counters still hit MySQL by design.
- For more dramatic performance results, the next benchmark target should be list/filter queries, read-heavy hot pages, or Redis-based view-count buffering with async batch flush.