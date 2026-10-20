@echo off
rem ===========================================================================
rem  Mall app - STOP
rem
rem  Kills ONLY the java process(es) belonging to THIS project, identified by
rem  "springboot-demo" appearing in the process command line.
rem
rem  Why not just kill whatever listens on port 8080?
rem    Because another project may be using that port. Killing by port would
rem    take down an unrelated application. Matching the command line is precise:
rem    this project's classes live under ...\springboot-demo\target\classes,
rem    so the path shows up in both the Maven launcher and the forked app.
rem
rem  NOTE: keep this file ASCII-only and do NOT call "chcp" here
rem        (switching code pages mid-file breaks cmd.exe parsing).
rem ===========================================================================
setlocal
cd /d "%~dp0"

echo.
echo   Looking for the mall app ^(springboot-demo^) ...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$procs = @(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*springboot-demo*' });" ^
  "if ($procs.Count -eq 0) { Write-Host '  Not running.'; exit 0 };" ^
  "foreach ($p in $procs) { Write-Host ('  stopping PID ' + $p.ProcessId); Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue };" ^
  "Start-Sleep -Milliseconds 900;" ^
  "$left = @(Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*springboot-demo*' });" ^
  "if ($left.Count -eq 0) { Write-Host ('  Stopped ' + $procs.Count + ' process(es).') } else { Write-Host ('  WARNING: ' + $left.Count + ' process(es) still alive.') }"

echo.
echo   Done. If the mall app was running, it has been stopped.
echo   Note: this script only touches this project. Other apps on the same
echo         port (e.g. other projects) are left alone on purpose.
echo.
pause
