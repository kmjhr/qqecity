# 冒烟测试：只读 GET 接口批量探测
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080/api'
$tokens = Get-Content D:\codex\codex-data\qingqi-ecity\.test-tokens.json -Raw | ConvertFrom-Json
$H = @{ Authorization = "Bearer $($tokens.testuser)" }
$Hb = @{ Authorization = "Bearer $($tokens.banker01)" }
$Ha = @{ Authorization = "Bearer $($tokens.admin)" }
$Hl = @{ Authorization = "Bearer $($tokens.landlord01)" }
$He = @{ Authorization = "Bearer $($tokens.entrepreneur)" }

function Probe($name, $method, $url, $headers, $body) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  try {
    $p = @{ Uri = $base + $url; Method = $method; Headers = $headers; TimeoutSec = 10 }
    if ($body) { $p.ContentType = 'application/json'; $p.Body = $body }
    $r = Invoke-RestMethod @p
    $sw.Stop()
    $dataLen = 0
    if ($null -ne $r.data) { $dataLen = ($r.data | ConvertTo-Json -Depth 4 -Compress).Length }
    "{0,-40} {1,-6} code={2,-5} {3,5}ms  datalen={4}" -f $name, $method, $r.code, $sw.ElapsedMilliseconds, $dataLen
  } catch {
    $sw.Stop()
    $resp = $_.Exception.Response
    $code = if ($resp) { [int]$resp.StatusCode } else { 'ERR' }
    "{0,-40} {1,-6} HTTP={2,-4} {3,5}ms  {4}" -f $name, $method, $code, $sw.ElapsedMilliseconds, $_.Exception.Message.Substring(0,[Math]::Min(60,$_.Exception.Message.Length))
  }
}

Write-Host "===== #18 chat 对话引擎 ====="
Probe 'chat.engine-status' GET '/v1/chat/engine-status' $H
Probe 'chat.history' GET '/v1/chat/history' $H

Write-Host "===== #3/#4 政策 ====="
Probe 'policy.match' GET '/v1/policy/match' $H
Probe 'policy.matched' GET '/v1/policy/matched' $H

Write-Host "===== #8 消息中心 ====="
Probe 'msg.page' GET '/v1/message/page?pageNum=1&pageSize=5' $H
Probe 'msg.unread' GET '/v1/message/unread-count' $H

Write-Host "===== #21 画像 ====="
Probe 'profile.get' GET '/v1/profile' $H

Write-Host "===== #19 流水聚合 ====="
Probe 'cashflow.auth.list' GET '/v1/cashflow/aggregate/auth' $He
Probe 'cashflow.report' GET '/v1/cashflow/aggregate/report' $He

Write-Host "===== #20 防刷单 ====="
Probe 'anti-brush.overview' GET '/v1/cashflow/anti-brush/overview' $He

Write-Host "===== #7 理财+测评 ====="
Probe 'risk-assess.questionnaire' GET '/v1/consumption/risk-assessment/questionnaire' $H
Probe 'risk-assess.latest' GET '/v1/consumption/risk-assessment/latest' $H
Probe 'finance.recommend' GET '/v1/consumption/finance-product/recommend' $H

Write-Host "===== #13/#14 消费治理 ====="
Probe 'high-freq.history' GET '/v1/consumption/high-freq-borrow/history' $H
Probe 'credit-monitor.latest' GET '/v1/consumption/credit-monitor/latest' $H
Probe 'credit-monitor.reports' GET '/v1/consumption/credit-monitor/reports' $H
Probe 'credit-monitor.warnings' GET '/v1/consumption/credit-monitor/warnings' $H
Probe 'consume-guide.bill' GET '/v1/consumption/guide/monthly-bill' $H
Probe 'consume-guide.pay-before' GET '/v1/consumption/guide/pay-before-reminder' $H

Write-Host "===== #1/#2/#15/#26 安全 ====="
Probe 'scenario.list' GET '/v1/safety/scenario/list' $H
Probe 'credit-report.reports' GET '/v1/safety/credit-report/reports' $H
Probe 'overdue.calendar' GET '/v1/safety/overdue-risk/calendar' $H
Probe 'overdue.warnings' GET '/v1/safety/overdue-risk/warnings' $H
Probe 'anti-fraud.list' GET '/v1/safety/anti-fraud/list' $H

Write-Host "===== #12 经营赋能 ====="
Probe 'cashflow-warn.history' GET '/v1/operation/cashflow-warning/history' $He
Probe 'insurance.matched' GET '/v1/insurance/matched' $He
Probe 'finance.content' GET '/v1/operation/finance/content' $He
Probe 'finance.gt.checklist' GET '/v1/operation/finance/individual-to-company/checklist' $He

Write-Host "===== #10/#11/#6 贷款 ====="
Probe 'loan.credit' GET '/v1/loan/credit' $He
Probe 'loan.applications' GET '/v1/loan/applications' $He
Probe 'loan.credit-txns' GET '/v1/loan/credit-txns' $He
Probe 'loan.observation' GET '/v1/loan/observation' $He
Probe 'loan.merchants' GET '/v1/loan/merchants' $He

Write-Host "===== #5 预算 ====="
Probe 'budget.list' GET '/v1/budget/list' $H
Probe 'budget.overview' GET '/v1/budget/overview' $H
Probe 'budget.categories' GET '/v1/budget/categories' $H
Probe 'budget.savings' GET '/v1/budget/savings' $H

Write-Host "===== #9 保函索赔 ====="
Probe 'claim.page' GET '/v1/guarantee/claims?pageNum=1&pageSize=5' $Hl
Probe 'claim.status-flow' GET '/v1/guarantee/claims/status-flow' $Hl
Probe 'claim.queue(banker)' GET '/v1/guarantee/claims/manual-review-queue' $Hb

Write-Host "===== #22 管理端 ====="
Probe 'admin.guarantee.queue' GET '/v1/admin/guarantee/manual-review-queue' $Hb
Probe 'admin.loan.apps' GET '/v1/admin/loan/applications' $Hb
Probe 'admin.loan.txns' GET '/v1/admin/loan/credit-txns' $Hb
Probe 'admin.entrust' GET '/v1/admin/loan/entrust-payments' $Hb
Probe 'admin.warnings' GET '/v1/admin/risk-warnings' $Hb
Probe 'admin.user.page' GET '/admin/v1/user/page?pageNum=1&pageSize=3' $Hb

Write-Host "===== 基础 ====="
Probe 'user.profile' GET '/v1/user/profile' $H
Probe 'bookkeeping.records' GET '/v1/bookkeeping/records' $He
Probe 'bookkeeping.cashflow' GET '/v1/bookkeeping/cashflow-report' $He
