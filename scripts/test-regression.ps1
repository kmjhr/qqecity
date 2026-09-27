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

# 修复回归验证：D2/D3/D5/D7 + chat
$base='http://localhost:8080/api'
$t = Get-Content D:\codex\codex-data\qingqi-ecity\.test-tokens.json -Raw | ConvertFrom-Json
# 重新登录拿最新 token（后端已重建，旧 token 仍有效但保险起见）
function Login($u){ (Invoke-RestMethod -Uri "$base/v1/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$u`",`"password`":`"123456`"}").data.accessToken }
$Tuser = Login 'testuser'; $Tent = Login 'entrepreneur'; $Tbank = Login 'banker01'
$H  = @{ Authorization = "Bearer $Tuser" }
$He = @{ Authorization = "Bearer $Tent" }
$Hb = @{ Authorization = "Bearer $Tbank" }
$Hl = @{ Authorization = "Bearer $(Login 'landlord01')" }
$Ha = @{ Authorization = "Bearer $(Login 'admin')" }

function Call($n,$m,$u,$h,$b){
  try {
    $p=@{Uri=$base+$u;Method=$m;Headers=$h;ContentType='application/json; charset=utf-8';TimeoutSec=12}
    if($b){$p.Body=$b}
    $r=Invoke-RestMethod @p
    $d = if ($null -ne $r.data) { $r.data | ConvertTo-Json -Depth 4 -Compress } else { '(data=null)' }
    "{0,-40} code={1,-5} {2}" -f $n,$r.code,$d.Substring(0,[Math]::Min(150,$d.Length))
  } catch { "{0,-40} ERR {1}" -f $n,$_.Exception.Message }
}

Write-Host "===== D2 banker 放行管理端 ====="
Call 'banker -> admin.loan.apps'      GET '/v1/admin/loan/applications' $Hb
Call 'banker -> admin.warnings'       GET '/v1/admin/risk-warnings' $Hb
Call 'banker -> admin.user.page'      GET '/admin/v1/user/page?pageNum=1&pageSize=2' $Hb
Call 'user 仍被拒(4001)'              GET '/v1/admin/risk-warnings' $H

Write-Host "`n===== D3 情景教学 ====="
Call 'scenario.list'                  GET '/v1/safety/scenario/list' $H
Call 'scenario.detail(5)'             GET '/v1/safety/scenario/5' $H
Call 'scenario.submit(5,全对)'        POST '/v1/safety/scenario/5/submit' $H '{"answers":{"1":"B","2":"B","3":"C"}}'
Call 'scenario.submit(5,选错纠偏)'    POST '/v1/safety/scenario/5/submit' $H '{"answers":{"1":"A","2":"B","3":"C"}}'

Write-Host "`n===== D5 防刷单对接聚合 ====="
Call 'aggregate.auth(WECHAT)'         POST '/v1/cashflow/aggregate/auth' $He '{"channels":["WECHAT","ICBC_QR"]}'
Call 'aggregate.report'                GET '/v1/cashflow/aggregate/report' $He
Call 'anti-brush.detect'               POST '/v1/cashflow/anti-brush/detect' $He '{}'

Write-Host "`n===== D7 非白名单商户 ====="
Call 'merchants.by-status(PENDING)'   GET '/v1/loan/merchants/by-status?verifyStatus=PENDING' $Hb

Write-Host "`n===== chat 对话（user-web 重建后验证后端）====="
Call 'chat.如何申请保函'               POST '/v1/chat/messages' $H '{"message":"如何申请保函"}'

Write-Host "`n===== 顺带抽查其余正常 ====="
Call 'profile.get'                    GET '/v1/profile' $H
Call 'policy.match'                   GET '/v1/policy/match' $He
Call 'loan.credit(entrepreneur)'      GET '/v1/loan/credit' $He
Call 'finance.recommend'              GET '/v1/consumption/finance-product/recommend' $H


