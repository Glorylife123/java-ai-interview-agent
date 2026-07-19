param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$RedisCli = "E:\Redis\redis-cli.exe",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$RedisPassword = "redis123",
    [string]$Username = "demo_user",
    [string]$Password = "123456",
    [long]$QuestionId = 0,
    [int]$Loops = 20,
    [string]$OutFile = "docs/demo/results/redis-demo-metrics.md",
    [string]$LimitUsername = "redis_limit_demo",
    [switch]$RunUpdateInvalidation,
    [string]$AdminUsername = "",
    [string]$AdminPassword = ""
)

$ErrorActionPreference = "Stop"

function Invoke-Redis {
    param([string[]]$RedisArgs)
    $out = & $RedisCli --no-auth-warning -h $RedisHost -p $RedisPort -a $RedisPassword @RedisArgs 2>$null
    return ($out -join "`n").Trim()
}

function Invoke-MeasuredRequest {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = ""
    )
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $jsonBody = $null
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 12
    }
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Method $Method -Uri ($BaseUrl + $Path) -Headers $headers `
            -ContentType "application/json" -Body $jsonBody -UseBasicParsing -ErrorAction Stop
        $sw.Stop()
        $json = $null
        if ($response.Content) {
            $json = $response.Content | ConvertFrom-Json
        }
        [pscustomobject]@{ Status = [int]$response.StatusCode; Ms = $sw.Elapsed.TotalMilliseconds; Json = $json; Raw = $response.Content }
    } catch {
        $sw.Stop()
        $status = 0
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        $raw = $_.ErrorDetails.Message
        $json = $null
        if ($raw) {
            try { $json = $raw | ConvertFrom-Json } catch { }
        }
        [pscustomobject]@{ Status = $status; Ms = $sw.Elapsed.TotalMilliseconds; Json = $json; Raw = $raw }
    }
}

function Average([double[]]$Values) {
    if (-not $Values -or $Values.Count -eq 0) { return 0 }
    return [math]::Round(($Values | Measure-Object -Average).Average, 2)
}

function Percentile([double[]]$Values, [double]$P) {
    if (-not $Values -or $Values.Count -eq 0) { return 0 }
    $sorted = $Values | Sort-Object
    $index = [math]::Ceiling(($P / 100) * $sorted.Count) - 1
    $index = [math]::Max(0, [math]::Min($index, $sorted.Count - 1))
    return [math]::Round($sorted[$index], 2)
}

function Sha256Hex([string]$Value) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
    $hash = $sha.ComputeHash($bytes)
    (($hash | ForEach-Object { $_.ToString("x2") }) -join "")
}

function Login([string]$Name, [string]$Pwd) {
    Invoke-MeasuredRequest -Method "POST" -Path "/api/auth/login" -Body @{ username = $Name; password = $Pwd }
}

if (-not (Test-Path $RedisCli)) {
    throw "redis-cli not found: $RedisCli"
}

$ping = Invoke-Redis -RedisArgs @("PING")
if ($ping -notmatch "PONG") {
    throw "Redis PING failed. Check RedisHost/RedisPort/RedisPassword. Output: $ping"
}

$login = Login $Username $Password
if ($login.Status -ne 200) {
    throw "Login failed for $Username. Register a demo user first or pass -Username/-Password. Status=$($login.Status), Body=$($login.Raw)"
}
$token = $login.Json.data.access_token
$userId = [long]$login.Json.data.user.id

if ($QuestionId -le 0) {
    $page = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions?pageNum=1&pageSize=1" -Token $token
    if ($page.Status -ne 200 -or -not $page.Json.data.records -or $page.Json.data.records.Count -eq 0) {
        throw "No question found. Pass -QuestionId or seed question data first."
    }
    $QuestionId = [long]$page.Json.data.records[0].id
}

$detailKey = "question:detail:$QuestionId"
$hotListKey = "question:hot:list"
$statKey = "user:practice:stat:$userId"
$submitLockKey = "answer:submit:lock:${userId}:$QuestionId"

Invoke-Redis -RedisArgs @("DEL", $detailKey, $hotListKey, $statKey, $submitLockKey) | Out-Null

$noCacheTimes = @()
for ($i = 0; $i -lt $Loops; $i++) {
    Invoke-Redis -RedisArgs @("DEL", $detailKey) | Out-Null
    $r = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token
    $noCacheTimes += $r.Ms
}

Invoke-Redis -RedisArgs @("DEL", $detailKey) | Out-Null
$warm = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token
$detailTtlAfterWarm = Invoke-Redis -RedisArgs @("TTL", $detailKey)
$cacheTimes = @()
for ($i = 0; $i -lt $Loops; $i++) {
    $r = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token
    $cacheTimes += $r.Ms
}

Invoke-Redis -RedisArgs @("EXPIRE", $detailKey, "2") | Out-Null
Start-Sleep -Seconds 3
$detailExistsAfterExpire = Invoke-Redis -RedisArgs @("EXISTS", $detailKey)
$afterExpire = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token
$detailExistsAfterReload = Invoke-Redis -RedisArgs @("EXISTS", $detailKey)

Invoke-Redis -RedisArgs @("DEL", $hotListKey) | Out-Null
# Ensure the rank has at least one member for this question.
Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token | Out-Null
$hotHits = 0
$hotMisses = 0
$hotTimes = @()
for ($i = 0; $i -lt $Loops; $i++) {
    $existsBefore = [int](Invoke-Redis -RedisArgs @("EXISTS", $hotListKey))
    if ($existsBefore -eq 1) { $hotHits++ } else { $hotMisses++ }
    $r = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/hot?limit=10" -Token $token
    $hotTimes += $r.Ms
}
$hotHitRate = if (($hotHits + $hotMisses) -eq 0) { 0 } else { [math]::Round(($hotHits * 100.0) / ($hotHits + $hotMisses), 2) }
$hotTtl = Invoke-Redis -RedisArgs @("TTL", $hotListKey)

$statBefore = Invoke-Redis -RedisArgs @("EXISTS", $statKey)
$stat1 = Invoke-MeasuredRequest -Method "GET" -Path "/api/stat/overview" -Token $token
$statTtl = Invoke-Redis -RedisArgs @("TTL", $statKey)
$stat2 = Invoke-MeasuredRequest -Method "GET" -Path "/api/stat/overview" -Token $token

$answerBody = @{ questionId = $QuestionId; userAnswer = "Redis demo answer"; timeCostSeconds = 3 }
Invoke-Redis -RedisArgs @("DEL", $submitLockKey, $statKey) | Out-Null
$submit1 = Invoke-MeasuredRequest -Method "POST" -Path "/api/answer/submit" -Token $token -Body $answerBody
$submitLockTtl = Invoke-Redis -RedisArgs @("TTL", $submitLockKey)
$statAfterSubmit = Invoke-Redis -RedisArgs @("EXISTS", $statKey)
$submit2 = Invoke-MeasuredRequest -Method "POST" -Path "/api/answer/submit" -Token $token -Body $answerBody

$limitStatuses = @()
for ($i = 0; $i -lt 6; $i++) {
    $bad = Login $LimitUsername "wrong-password"
    $limitStatuses += $bad.Status
}
$limitKey127 = "interview:login:fail:" + (Sha256Hex ($LimitUsername.ToLower() + [char]0 + "127.0.0.1"))
$limitKeyV6 = "interview:login:fail:" + (Sha256Hex ($LimitUsername.ToLower() + [char]0 + "0:0:0:0:0:0:0:1"))
$limitTtl127 = Invoke-Redis -RedisArgs @("TTL", $limitKey127)
$limitTtlV6 = Invoke-Redis -RedisArgs @("TTL", $limitKeyV6)

$updateInvalidationLine = "Not run. Pass -RunUpdateInvalidation -AdminUsername <admin> -AdminPassword <password> to verify PUT invalidates question:detail:{id}."
if ($RunUpdateInvalidation) {
    if (-not $AdminUsername -or -not $AdminPassword) {
        throw "RunUpdateInvalidation requires -AdminUsername and -AdminPassword"
    }
    $adminLogin = Login $AdminUsername $AdminPassword
    if ($adminLogin.Status -ne 200) {
        throw "Admin login failed. Status=$($adminLogin.Status), Body=$($adminLogin.Raw)"
    }
    $adminToken = $adminLogin.Json.data.access_token
    $detail = Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token
    Invoke-Redis -RedisArgs @("DEL", $detailKey) | Out-Null
    Invoke-MeasuredRequest -Method "GET" -Path "/api/questions/$QuestionId" -Token $token | Out-Null
    $existsBeforeUpdate = Invoke-Redis -RedisArgs @("EXISTS", $detailKey)
    $updateBody = @{}
    $update = Invoke-MeasuredRequest -Method "PUT" -Path "/api/questions/$QuestionId" -Token $adminToken -Body $updateBody
    $existsAfterUpdate = Invoke-Redis -RedisArgs @("EXISTS", $detailKey)
    $updateInvalidationLine = "PUT status=$($update.Status), detail key before update=$existsBeforeUpdate, after update=$existsAfterUpdate."
}

$noAvg = Average $noCacheTimes
$hitAvg = Average $cacheTimes
$improvement = if ($noAvg -gt 0) { [math]::Round((($noAvg - $hitAvg) * 100.0) / $noAvg, 2) } else { 0 }

$report = @"
# Redis Demo Metrics

Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
BaseUrl: $BaseUrl
QuestionId: $QuestionId
Loops: $Loops

## Interview-ready Numbers

| Metric | Result |
| --- | --- |
| Question detail no-cache avg | ${noAvg} ms |
| Question detail cache-hit avg | ${hitAvg} ms |
| Question detail latency improvement | ${improvement}% |
| Question detail p95 no-cache | $(Percentile $noCacheTimes 95) ms |
| Question detail p95 cache-hit | $(Percentile $cacheTimes 95) ms |
| MySQL query count, no-cache path | 4 mapper calls: select question, select tags, update view_count, select counters |
| MySQL query count, cache-hit path | 2 mapper calls: update view_count, select counters |
| MySQL query reduction | 50% on detail cache hit path |
| Hot question cache hit rate | ${hotHitRate}% (${hotHits} hits / $($hotHits + $hotMisses) requests) |
| Login failure limit effect | statuses: $($limitStatuses -join ', ') ; expected first failures 401, sixth 429 |
| Duplicate submit lock effect | first submit status=$($submit1.Status), second submit status=$($submit2.Status), lock TTL=${submitLockTtl}s |

## Redis Evidence

| Key | Evidence |
| --- | --- |
| $detailKey | TTL after warm=${detailTtlAfterWarm}s; exists after forced 2s expiry=${detailExistsAfterExpire}; exists after reload=${detailExistsAfterReload}; reload status=$($afterExpire.Status) |
| $hotListKey | TTL after hot query=${hotTtl}s |
| $statKey | exists before stat=${statBefore}; TTL after stat=${statTtl}s; status1=$($stat1.Status); status2=$($stat2.Status); exists after answer submit=${statAfterSubmit} |
| $submitLockKey | TTL after first submit=${submitLockTtl}s |
| login failure key | TTL 127.0.0.1=${limitTtl127}s; TTL IPv6 loopback=${limitTtlV6}s |
| update invalidation | ${updateInvalidationLine} |

## Notes

- Detail cache hit still updates view_count and reads dynamic counters from MySQL, so the optimized path is not zero-SQL by design.
- Hot list hit rate is measured by checking whether `question:hot:list` existed immediately before each `/api/questions/hot` request.
- For stable resume numbers, run this script after JVM warm-up and with a fixed local MySQL/Redis environment.
"@

$dir = Split-Path $OutFile -Parent
if ($dir) { New-Item -ItemType Directory -Force $dir | Out-Null }
[System.IO.File]::WriteAllText($OutFile, $report, [System.Text.UTF8Encoding]::new($false))
Write-Host "Metrics written to $OutFile"
Write-Host $report