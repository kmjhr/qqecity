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

# 异常 + 安全电池
$base='http://localhost:8080/api'
$t = Get-Content D:\codex\codex-data\qingqi-ecity\.test-tokens.json -Raw | ConvertFrom-Json
$H  = @{ Authorization = "Bearer $($t.testuser)" }   # uid=2 USER
$He = @{ Authorization = "Bearer $($t.entrepreneur)" } # uid=3 USER
$Hl = @{ Authorization = "Bearer $($t.landlord01)" }   # uid=4 LANDLORD
$Hb = @{ Authorization = "Bearer $($t.banker01)" }     # uid=5 BANK_OPERATOR
$Ha = @{ Authorization = "Bearer $($t.admin)" }        # uid=1 ADMIN

function Call($n,$m,$u,$h,$b){
  $sw=[Diagnostics.Stopwatch]::StartNew()
  try {
    $p=@{Uri=$base+$u;Method=$m;Headers=$h;ContentType='application/json; charset=utf-8';TimeoutSec=10}
    if($b){$p.Body=$b}
    $r=Invoke-RestMethod @p; $sw.Stop()
    "{0,-42} code={1,-5} msg={2}  {3}ms" -f $n,$r.code,$r.message,$sw.ElapsedMilliseconds
  } catch {
    $sw.Stop()
    $sc='?'; if($_.Exception.Response){$sc=[int]$_.Exception.Response.StatusCode}
    "{0,-42} HTTP={1} {2}ms" -f $n,$sc,$sw.ElapsedMilliseconds
  }
}

Write-Host "===== S1 无 token 访问受保护接口（应 401/1002）====="
Call 'no-token /v1/profile'            GET '/v1/profile' @{}
Call 'no-token /v1/chat/history'        GET '/v1/chat/history' @{}
Call 'no-token /v1/admin/risk-warnings' GET '/v1/admin/risk-warnings' @{}

Write-Host "`n===== S2 普通用户访问管理端（应 4001 越权）====="
Call 'user -> /v1/admin/loan/applications'  GET '/v1/admin/loan/applications' $H
Call 'user -> /v1/admin/risk-warnings'      GET '/v1/admin/risk-warnings' $H
Call 'user -> /admin/v1/user/page'          GET '/admin/v1/user/page?pageNum=1&pageSize=2' $H
Call 'landlord -> /v1/admin/loan/app'       GET '/v1/admin/loan/applications' $Hl

Write-Host "`n===== S3 banker(BANK_OPERATOR) 访问 /v1/admin 台（计划要求 banker01 可进）====="
Call 'banker -> /v1/admin/loan/applications' GET '/v1/admin/loan/applications' $Hb
Call 'banker -> /v1/admin/risk-warnings'    GET '/v1/admin/risk-warnings' $Hb

Write-Host "`n===== S4 跨用户数据隔离（越权访问他人数据应 4001）====="
Call 'user(2) 查 landlord(4) 索赔1详情' GET '/v1/guarantee/claims/1' $H
Call 'user(2) 查他人消息分页'           GET '/v1/message/page?pageNum=1&pageSize=3' $He
Call 'user(2) 标记他人消息已读(1)'      PUT '/v1/message/1/read' $He

Write-Host "`n===== S5 伪造/异常业务 ====="
Call 'chat 空消息(应 1001/校验)'  POST '/v1/chat/messages' $H '{"message":""}'
Call 'chat 超长消息(>500字)'      POST '/v1/chat/messages' $H ("{`"message`":`"" + ('测'*501) + "`"}")
Call 'claim 非房东提交索赔(用户2)' POST '/v1/guarantee/claims' $H '{"guaranteeId":1,"claimAmount":100,"claimReason":"x","evidenceFiles":"[]"}'
Call 'policy.push 普通用户(自推送)' POST '/v1/policy/push' $H '{}'

Write-Host "`n===== S6 伪造 JWT / 坏 token ====="
Call '篡改签名 token' GET '/v1/profile' @{ Authorization = 'Bearer eyJhbGciOiJIUzI1NiJ9.fake.signature' }
Call 'Bearer 缺失'    GET '/v1/profile' @{ Authorization = 'Bearer ' }


