# Redis Demo Metrics Template

Run `docs/demo/scripts/redis-demo-benchmark.ps1` to generate the real report at `docs/demo/results/redis-demo-metrics.md`.

| Metric | Value | Evidence |
| --- | --- | --- |
| Question detail no-cache avg | TODO ms | Clear `question:detail:{id}` before each request |
| Question detail cache-hit avg | TODO ms | Warm cache once, then repeat GET `/api/questions/{id}` |
| Question detail p95 no-cache | TODO ms | Script output |
| Question detail p95 cache-hit | TODO ms | Script output |
| MySQL query count no-cache | 4 mapper calls | `selectById`, `selectTagsByQuestionId`, `increaseViewCount`, `selectCountersById` |
| MySQL query count cache-hit | 2 mapper calls | `increaseViewCount`, `selectCountersById` |
| Hot question cache hit rate | TODO % | `question:hot:list` exists before request |
| Login failure limit | TODO | Expected 401 x5, then 429 |
| Duplicate submit lock | TODO | Expected first submit success, immediate second submit 429 |

Resume wording after filling numbers:

```text
引入 Redis Cache Aside 优化题目详情接口，将命中路径 MySQL mapper 调用由 4 次降至 2 次；在本地压测中题目详情平均耗时由 X ms 降至 Y ms，并通过 TTL、更新失效、登录失败限流和重复提交锁验证缓存一致性与降级可用性。
```