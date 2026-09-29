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

# 100 并发压测：列表/画像/审核接口 P95
$base='http://localhost:8080/api'
$t = Get-Content D:\codex\codex-data\qingqi-ecity\.test-tokens.json -Raw | ConvertFrom-Json

$targets = @(
  @{ name='profile.画像';        url='/v1/profile';                              token=$t.testuser },
  @{ name='guarantee.page列表';  url='/v1/guarantee/page?pageNum=1&pageSize=10'; token=$t.testuser },
  @{ name='policy.match';        url='/v1/policy/match';                         token=$t.entrepreneur },
  @{ name='admin.warnings审核';  url='/v1/admin/risk-warnings?pageNum=1&pageSize=10'; token=$t.admin }
)

$runspacePool = [RunspaceFactory]::CreateRunspacePool(1,30)
$runspacePool.Open()

foreach ($tg in $targets) {
  $urls = 1..100 | ForEach-Object { $base + $tg.url }
  $script = {
    param($u,$tok)
    $sw=[Diagnostics.Stopwatch]::StartNew()
    try {
      $r = Invoke-WebRequest -Uri $u -Headers @{Authorization="Bearer $tok"} -TimeoutSec 15 -UseBasicParsing
      $sw.Stop()
      [PSCustomObject]@{ms=$sw.ElapsedMilliseconds; code=$r.StatusCode; ok=$true}
    } catch { $sw.Stop(); [PSCustomObject]@{ms=$sw.ElapsedMilliseconds; code=0; ok=$false} }
  }
  $jobs = foreach($u in $urls){
    $ps = [PowerShell]::Create(); $ps.RunspacePool=$runspacePool
    [void]$ps.AddScript($script).AddArgument($u).AddArgument($tg.token)
    @{ pipe=$ps; result=$ps.BeginInvoke() }
  }
  $lat = foreach($j in $jobs){ $j.pipe.EndInvoke($j.result); $j.pipe.Dispose() }
  $ms = $lat | Where-Object ok | ForEach-Object { $_.ms } | Sort-Object
  $err = ($lat | Where-Object { -not $_.ok }).Count
  $n=$ms.Count
  $p50=$ms[[int]($n*0.5)]; $p95=$ms[[int]($n*0.95)]; $max=$ms[-1]
  "{0,-22} n={1} err={2}  P50={3}ms  P95={4}ms  MAX={5}ms  P95<1s={6}" -f $tg.name,$n,$err,$p50,$p95,$max, $(if($p95 -lt 1000){'PASS'}else{'FAIL'})
}
$runspacePool.Close()


