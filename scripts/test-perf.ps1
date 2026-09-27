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
