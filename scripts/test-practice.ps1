# L3 对话式反诈演练 · 接口全路径测试
# 覆盖：开始/回合（识破、被诱骗、中性）/结束/历史/回放 + 异常（未登录/越权/类型错/重复提交/空参）
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = 'http://localhost:8080/api'

function Post-Json($url, $token, $body) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    $json = if ($body) { $body | ConvertTo-Json -Compress } else { '{}' }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    try {
        $resp = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $bytes -ContentType 'application/json; charset=utf-8'
        return $resp
    } catch {
        return @{ code = [int]$_.Exception.Response.StatusCode; message = $_.Exception.Message }
    }
}
function Get-Json($url, $token) {
    $headers = @{}
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    try {
        return Invoke-RestMethod -Uri $url -Method Get -Headers $headers
    } catch {
        return @{ code = [int]$_.Exception.Response.StatusCode; message = $_.Exception.Message }
    }
}

# ---------- 登录两个账号 ----------
$login = Post-Json "$base/v1/auth/login" $null @{ username = 'testuser'; password = '123456' }
$token = $login.data.accessToken
$login2 = Post-Json "$base/v1/auth/login" $null @{ username = 'entrepreneur'; password = '123456' }
$token2 = $login2.data.accessToken
Write-Host "== login testuser: code=$($login.code) / entrepreneur: code=$($login2.code)"

# ---------- 1. 情景列表：应含 3 条 SCENARIO_SIM + 4 条 SCENARIO_DIALOG ----------
$list = Get-Json "$base/v1/safety/scenario/list" $token
$sim = @($list.data | Where-Object { $_.contentType -eq 'SCENARIO_SIM' })
$dlg = @($list.data | Where-Object { $_.contentType -eq 'SCENARIO_DIALOG' })
Write-Host "== list: total=$($list.data.Count) SIM=$($sim.Count) DIALOG=$($dlg.Count) (期望 7 / 3 / 4)"
$dlg | ForEach-Object { Write-Host "   - id=$($_.id) $($_.title)" }

# ---------- 2. 开始演练（情景 8 刷单） ----------
$start = Post-Json "$base/v1/safety/scenario/8/practice/start" $token $null
$prac = $start.data.practiceNo
Write-Host "== start(8): code=$($start.code) prac=$prac opening=$($start.data.openingLine) sim=$($start.data.simulated)"

# ---------- 3. 回合 · 路径A：一句识破 → SAFE ----------
$t1 = Post-Json "$base/v1/safety/scenario/practice/$prac/turn" $token @{ content = '你们是骗子吧，我要报警了，挂断！' }
Write-Host "== turn-A: code=$($t1.code) score=$($t1.data.safeScore) lvl=$($t1.data.riskLevel) gameOver=$($t1.data.gameOver) result=$($t1.data.result)"

# ---------- 4. 回合 · 路径B：连续危险 → LURED（新演练 情景9 公检法） ----------
$start9 = Post-Json "$base/v1/safety/scenario/9/practice/start" $token $null
$prac9 = $start9.data.practiceNo
$t9a = Post-Json "$base/v1/safety/scenario/practice/$prac9/turn" $token @{ content = '我下载安全防护App，开启屏幕共享配合你们清查' }
$t9b = Post-Json "$base/v1/safety/scenario/practice/$prac9/turn" $token @{ content = '我把钱转到安全账户验资，快点办' }
Write-Host "== turn-B1: code=$($t9a.code) score=$($t9a.data.safeScore) lvl=$($t9a.data.riskLevel) gameOver=$($t9a.data.gameOver) hint=$($t9a.data.dangerHint)"
Write-Host "== turn-B2: code=$($t9b.code) score=$($t9b.data.safeScore) lvl=$($t9b.data.riskLevel) gameOver=$($t9b.data.gameOver) result=$($t9b.data.result)"

# ---------- 5. 回合 · 路径C：中性 → 不结束；finish → FINISHED（情景10 征信） ----------
$start10 = Post-Json "$base/v1/safety/scenario/10/practice/start" $token $null
$prac10 = $start10.data.practiceNo
$t10 = Post-Json "$base/v1/safety/scenario/practice/$prac10/turn" $token @{ content = '哦，那需要我提供什么材料？' }
$fin = Post-Json "$base/v1/safety/scenario/practice/$prac10/finish" $token @{ reason = 'GIVE_UP' }
Write-Host "== turn-C: code=$($t10.code) score=$($t10.data.safeScore) lvl=$($t10.data.riskLevel) gameOver=$($t10.data.gameOver)"
Write-Host "== finish: code=$($fin.code) result=$($fin.data.result) name=$($fin.data.resultName) risk=$($fin.data.riskScore) wl=$($fin.data.warningLevel) points=$($fin.data.reviewPoints.Count)"

# ---------- 6. 历史 / 回放 ----------
$hist = Get-Json "$base/v1/safety/scenario/practice/history?pageNum=1&pageSize=10" $token
Write-Host "== history: code=$($hist.code) total=$($hist.data.total) rows=$($hist.data.list.Count)"
$hist.data.list | ForEach-Object { Write-Host "   - $($_.practiceNo) $($_.scenarioTitle) $($_.result) risk=$($_.riskScore)" }
$det = Get-Json "$base/v1/safety/scenario/practice/$prac9" $token
Write-Host "== detail($prac9): code=$($det.code) result=$($det.data.result) rounds=$($det.data.rounds.Count)"
$det.data.rounds | ForEach-Object { Write-Host "   - #$($_.roundNo) [$($_.speaker)] score=$($_.safeScore) hit=$($_.hitWords) $($_.content)" }

# ---------- 7. 异常与安全 ----------
$r1 = Post-Json "$base/v1/safety/scenario/practice/$prac/turn" $null @{ content = '没登录也来试试' }
Write-Host "== 未登录 turn: code=$($r1.code) (期望 1002)"
$r2 = Post-Json "$base/v1/safety/scenario/practice/$prac/turn" $token2 @{ content = '越权试试' }
Write-Host "== 越权 turn(entrepreneur→testuser): code=$($r2.code) (期望 4001)"
$r3 = Post-Json "$base/v1/safety/scenario/5/practice/start" $token $null
Write-Host "== 非对话情景 start(5 SCENARIO_SIM): code=$($r3.code) (期望 3001)"
$r4 = Post-Json "$base/v1/safety/scenario/practice/PRAC_NOT_EXIST/turn" $token @{ content = 'x' }
Write-Host "== 不存在演练: code=$($r4.code) (期望 3001)"
$r5 = Post-Json "$base/v1/safety/scenario/practice/$prac/turn" $token @{ content = '演练已结束还发言' }
Write-Host "== 已结束再 turn: code=$($r5.code) (期望 3001)"
$r6 = Post-Json "$base/v1/safety/scenario/practice/$prac10/turn" $token @{ content = '' }
Write-Host "== 空发言: code=$($r6.code) (期望 1001)"
