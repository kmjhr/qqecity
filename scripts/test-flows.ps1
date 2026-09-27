# ---- 中文显示/读写统一 UTF-8（避免终端乱码）----
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
cmd /c chcp 65001 | Out-Null
# ---- UTF-8 响应解码修复：遮蔽 Invoke-RestMethod，改用 curl.exe（PS5.1 按 Latin-1 误解码 UTF-8 响应 + 双引号参数丢失 的根治方案）----
function Invoke-RestMethod {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [string]$Method = 'GET',
        [hashtable]$Headers = @{},
        [object]$Body,
        [string]$ContentType = 'application/json; charset=utf-8',
        [int]$TimeoutSec = 0
    )
    $Method = $Method.ToUpperInvariant()
    $bodyStr = $null
    if ($null -ne $Body) {
        if ($Body -is [byte[]]) { $bodyStr = [Text.Encoding]::UTF8.GetString($Body) }
        elseif ($Body -is [string]) { $bodyStr = $Body }
        else { $bodyStr = $Body | ConvertTo-Json -Compress }
    }
    $argsList = @('-s')
    if ($null -ne $bodyStr) {
        $argsList += @('-d', '@-')
    } elseif ($Method -eq 'POST') {
        $argsList += @('-X', 'POST')
    }
    if ($TimeoutSec -gt 0) { $argsList += @('--max-time', "$TimeoutSec") }
    $argsList += @('-H', 'Content-Type: application/json; charset=utf-8')
    foreach ($k in $Headers.Keys) {
        $argsList += @('-H', ($k + ': ' + $Headers[$k]))
    }
    $argsList += $Uri
    if ($null -ne $bodyStr) {
        $raw = ($bodyStr | & curl.exe @argsList) -join ''
    } else {
        $raw = (& curl.exe @argsList) -join ''
    }
    if ([string]::IsNullOrWhiteSpace($raw)) { return $null }
    if ($raw -match '\u00e5|Ã|â€|æ') {
        try {
            $bytes = [Text.Encoding]::GetEncoding(28591).GetBytes($raw)
            $fixed = [Text.Encoding]::UTF8.GetString($bytes)
            if ($fixed -notmatch '\uFFFD') { $raw = $fixed }
        } catch {}
    }
    return ($raw | ConvertFrom-Json)
}

# 23项功能流测试（快乐路径）
$ErrorActionPreference='Continue'
$base='http://localhost:8080/api'
function Login($u){ (Invoke-RestMethod -Uri "$base/v1/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$u`",`"password`":`"123456`"}").data.accessToken }
$H  = @{ Authorization = "Bearer $(Login 'testuser')" }
$He = @{ Authorization = "Bearer $(Login 'entrepreneur')" }
$Hl = @{ Authorization = "Bearer $(Login 'landlord01')" }
$Hb = @{ Authorization = "Bearer $(Login 'banker01')" }
$Ha = @{ Authorization = "Bearer $(Login 'admin')" }

function Call($name, $method, $url, $headers, $bodyObj) {
  $body = if ($bodyObj) { ($bodyObj | ConvertTo-Json -Depth 6 -Compress) } else { $null }
  try {
    $p = @{ Uri=$base+$url; Method=$method; Headers=$headers; TimeoutSec=10; ContentType='application/json; charset=utf-8'; Body=$body }
    $r = Invoke-RestMethod @p
    $data = if ($null -ne $r.data) { ($r.data | ConvertTo-Json -Depth 5 -Compress) } else { '' }
    "{0,-34} {1,-4} code={2,-5} msg={3}  data={4}" -f $name,$method,$r.code,$r.message, ($data.Substring(0,[Math]::Min(180,$data.Length)))
  } catch {
    "{0,-34} {1,-4} ERR {2}" -f $name,$method,$_.Exception.Message
  }
}

Write-Host "===== #18 对话引擎（原 bug 修复验证）====="
Call '#18 chat.你好'           POST '/v1/chat/messages' $H  @{message='你好'}
Call '#18 chat.如何申请保函'   POST '/v1/chat/messages' $H  @{message='如何申请保函'}
Call '#18 chat.刷单诈骗怎么办' POST '/v1/chat/messages' $H  @{message='刷单诈骗怎么办'}

Write-Host "`n===== #1 反诈情景教学 ====="
Call '#1 scenario.list'   GET '/v1/safety/scenario/list' $H
Call '#1 scenario.detail(1)' GET '/v1/safety/scenario/1' $H
Call '#1 scenario.submit' POST '/v1/safety/scenario/1/submit' $H @{answers=@{'1'='B';'2'='A'}}

Write-Host "`n===== #2 征信报告解读 ====="
Call '#2 credit.load-demo' POST '/v1/safety/credit-report/load-demo' $H @{}
Call '#2 credit.interpret(1)' GET '/v1/safety/credit-report/1/interpret' $H

Write-Host "`n===== #3/#4 政策匹配/推送 ====="
Call '#3 policy.match'    GET '/v1/policy/match' $He
Call '#4 policy.push'     POST '/v1/policy/push' $He @{}

Write-Host "`n===== #7 理财+风险测评 ====="
Call '#7 recommend(未测评,应拦)' GET '/v1/consumption/finance-product/recommend' $H
Call '#7 assessment.submit' POST '/v1/consumption/risk-assessment/submit' $H @{answers=@{'1'='A';'2'='A';'3'='A';'4'='A';'5'='A';'6'='A';'7'='A';'8'='A';'9'='A';'10'='A'}}
Call '#7 recommend(已测评)' GET '/v1/consumption/finance-product/recommend' $H

Write-Host "`n===== #10 A类循环贷 提款→还款 ====="
Call '#10 withdraw 1000' POST '/v1/loan/withdraw' $He @{amount=1000; purpose='备货'}
Call '#10 credit-txns'   GET '/v1/loan/credit-txns' $He
Call '#10 repay 500'     POST '/v1/loan/repay' $He @{amount=500}

Write-Host "`n===== #11 B转A观察期 ====="
Call '#11 observation'       GET '/v1/loan/observation' $He
Call '#11 observation.advance' POST '/v1/loan/observation/advance' $He @{}

Write-Host "`n===== #12/#13/#14/#15 预警类 detect ====="
Call '#12 cashflow.detect'      POST '/v1/operation/cashflow-warning/detect' $He @{}
Call '#13 highfreq.detect'      POST '/v1/consumption/high-freq-borrow/detect' $H @{}
Call '#14 creditmon.softquery'  POST '/v1/consumption/credit-monitor/soft-query' $H @{}
Call '#15 overdue.predict'      POST '/v1/safety/overdue-risk/predict' $H @{}

Write-Host "`n===== #19 流水聚合 ====="
Call '#19 agg.auth(wechat)' POST '/v1/cashflow/aggregate/auth' $He @{channelCode='WECHAT'}
Call '#19 agg.report'       GET '/v1/cashflow/aggregate/report' $He
Call '#19 agg.unbind'       DELETE '/v1/cashflow/aggregate/auth/WECHAT' $He

Write-Host "`n===== #20 防刷单 ====="
Call '#20 anti-brush.detect' POST '/v1/cashflow/anti-brush/detect' $He @{txnCount=200; opponentCount=2; lateNightRatio=0.8; refundRatio=0.3}

Write-Host "`n===== #9 G-6 索赔闭环（landlord01 对 guaranteeId=1 索赔）====="
Call '#9 claim.submit'   POST '/v1/guarantee/claims' $Hl @{guaranteeId=1; claimAmount=2000; claimReason='租客欠租2个月且损坏墙面'; evidenceFiles='["欠租记录","损坏照片","物品清单"]'}


