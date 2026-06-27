Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Push-Location -LiteralPath $PSScriptRoot
function ShowHelp {
  Write-Host "Usage: .\run_test.ps1 [options]"
  Write-Host ""
  Write-Host "Options:"
  Write-Host "  -h, --help          Show help"
  Write-Host "  -b, --build-only    Build only"
  Write-Host "  --skip-build        Skip build"
  exit 0
}
function ResolvePython {
  $script:PythonExe = $null
  if (Get-Command python3 -ErrorAction SilentlyContinue) { $script:PythonExe = "python3" }
  elseif (Get-Command python -ErrorAction SilentlyContinue) { $script:PythonExe = "python" }
  elseif (Get-Command py -ErrorAction SilentlyContinue) {
    try { & py -3 -c "import sys" > $null 2>&1; if ($LASTEXITCODE -eq 0) { $script:PythonExe = "py -3" } } catch {}
  }
  if (-not $script:PythonExe) { throw "Python 3 not found in PATH" }
}
function CheckPython {
  Write-Host "[Check] Python..."
  ResolvePython
  Write-Host "[Check] Using: $script:PythonExe"
}
function BuildKmp {
  Write-Host ""
  Write-Host "[Step 1/3] Build KMP artifacts"
  Write-Host "========================================"
  $start = Get-Date
  Write-Host "Building..."
  & ".\gradlew.bat" ":composeApp:publishDebugBinariesToHarmonyApp"
  $elapsed = [int]((Get-Date) - $start).TotalSeconds
  Write-Host "[Done] Build ok ($elapsed s)"
}
function CheckDevice {
  Write-Host ""
  Write-Host "[Device] Check hdc device..."
  if (-not (Get-Command hdc -ErrorAction SilentlyContinue)) {
    Write-Host "[Warn] hdc not found in PATH" -ForegroundColor Yellow
    return $false
  }
  $dev = & hdc list targets
  if ([string]::IsNullOrWhiteSpace($dev)) {
    Write-Host "[Error] No device found" -ForegroundColor Red
    return $false
  }
  Write-Host "[Device] Found:"
  Write-Host $dev
  return $true
}
function DeployApp {
  param([bool]$SkipBuild)
  $step = if ($SkipBuild) { 1 } else { 2 }
  Write-Host ""
  Write-Host "[Step $step] Deploy app"
  Write-Host "========================================"
  $hap = Join-Path $PSScriptRoot "harmonyApp/entry/build/default/outputs/default/entry-default-signed.hap"
  if (Test-Path $hap) {
    Write-Host "Install hap..."
    try {
      & hdc install -r "$hap" > $null 2>&1
      Write-Host "[Done] Installed"
      Write-Host "Start app..."
      & hdc shell aa start -a EntryAbility -b com.example.harmonyapp > $null 2>&1
      Write-Host "[Done] App started"
      Start-Sleep -Seconds 3
      return
    } catch {
      Write-Host "[Warn] Auto install failed" -ForegroundColor Yellow
    }
  } else {
    Write-Host "[Warn] Hap not found: $hap" -ForegroundColor Yellow
  }
  Write-Host ""
  Write-Host "Deploy via DevEco, then press ENTER to continue..."
  [void][Console]::ReadLine()
  Write-Host "[Continue]"
}
function RunAutoTest {
  param([bool]$SkipBuild)
  $step = if ($SkipBuild) { 2 } else { 3 }
  Write-Host ""
  Write-Host "[Step $step] Run automation"
  Write-Host "========================================"
  New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot "test_reports") > $null 2>&1
  & $script:PythonExe (Join-Path $PSScriptRoot "autotest.py")
}
function Main {
  $mode = "full"
  $skipBuild = $false
  $i = 0
  while ($i -lt $args.Count) {
    $a = $args[$i]
    switch ($a) {
      "-h" { ShowHelp }
      "--help" { ShowHelp }
      "-b" { $mode = "build" }
      "--build-only" { $mode = "build" }
      "--skip-build" { $skipBuild = $true }
      default { ShowHelp }
    }
    $i++
  }
  CheckPython
  if ($mode -eq "full" -and -not $skipBuild) {
    Write-Host ""
    Write-Host "[Tip]"
    Write-Host "  If artifacts already exist, use --skip-build"
    Start-Sleep -Seconds 1
  }
  switch ($mode) {
    "build" {
      BuildKmp
    }
    "full" {
      if (-not $skipBuild) { BuildKmp }
      else { Write-Host ""; Write-Host "[Skip] use existing artifacts" }
      if (-not (CheckDevice)) {
        Write-Host "[Warn] No device, will continue..." -ForegroundColor Yellow
      }
      DeployApp -SkipBuild:$skipBuild
      RunAutoTest -SkipBuild:$skipBuild
    }
  }
  Write-Host ""
  Write-Host "========================================"
  Write-Host "[Done] Automation finished"
  Write-Host "========================================"
  Write-Host ""
  Write-Host "Reports:"
  Write-Host "  - HTML: test_reports\test_report.html"
  Write-Host "  - CSV:  test_reports\test_report.csv"
}
Main
Pop-Location
