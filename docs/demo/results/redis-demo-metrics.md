# Redis Demo Metrics

Generated: 2026-07-19 23:09:38
BaseUrl: http://localhost:8080
QuestionId: 16
Loops: 50

## Interview-ready Numbers

| Metric | Result |
| --- | --- |
| Question detail no-cache avg | 38.39 ms |
| Question detail cache-hit avg | 34.11 ms |
| Question detail latency improvement | 11.15% |
| Question detail p95 no-cache | 49.33 ms |
| Question detail p95 cache-hit | 37.95 ms |
| MySQL query count, no-cache path | 4 mapper calls: select question, select tags, update view_count, select counters |
| MySQL query count, cache-hit path | 2 mapper calls: update view_count, select counters |
| MySQL query reduction | 50% on detail cache hit path |
| Hot question cache hit rate | 98% (49 hits / 50 requests) |
| Login failure limit effect | statuses: 401, 401, 401, 401, 401, 429 ; expected first failures 401, sixth 429 |
| Duplicate submit lock effect | first submit status=200, second submit status=429, lock TTL=10s |

## Redis Evidence

| Key | Evidence |
| --- | --- |
| question:detail:16 | TTL after warm=1193s; exists after forced 2s expiry=0; exists after reload=1; reload status=200 |
| question:hot:list | TTL after hot query=296s |
| user:practice:stat:4 | exists before stat=0; TTL after stat=300s; status1=200; status2=200; exists after answer submit=0 |
| answer:submit:lock:4:16 | TTL after first submit=10s |
| login failure key | TTL 127.0.0.1=-2s; TTL IPv6 loopback=300s |
| update invalidation | PUT status=200, detail key before update=1, after update=0. |

## Notes

- Detail cache hit still updates view_count and reads dynamic counters from MySQL, so the optimized path is not zero-SQL by design.
- Hot list hit rate is measured by checking whether question:hot:list existed immediately before each /api/questions/hot request.
- For stable resume numbers, run this script after JVM warm-up and with a fixed local MySQL/Redis environment.