@echo off
rem ===========================================================================
rem  Mall app - START
rem
rem  Usage:
rem      start.cmd           start on port 8080
rem      start.cmd 8081      start on another port (when 8080 is taken)
rem
rem  Stop it with:  Ctrl+C in this window,  or  stop.cmd in another window.
rem
rem  NOTE 1: keep this file ASCII-only and do NOT call "chcp" here.
rem          cmd.exe reads a batch file line by line using the CURRENT code page,
rem          so switching the code page mid-file makes it lose sync and try to
rem          execute garbled text as commands.
rem
rem  NOTE 2: Windows tools are called with their full path (%SystemRoot%\System32).
rem          Inside a Git-Bash / MSYS PATH, a bare "find" resolves to GNU find
rem          instead of Windows find.exe, which produces bogus results.
rem ===========================================================================
setlocal
cd /d "%~dp0"

set "SYS32=%SystemRoot%\System32"
set "FINDSTR=%SYS32%\findstr.exe"

rem ---- port (optional first argument) ----
set "PORT=%~1"
if "%PORT%"=="" set "PORT=8080"

rem ---- toolchain: change these two lines on another machine ----
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_161"
set "MVN=C:\Tools\apache-maven-3.9.12\bin\mvn.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] JDK 8 not found: %JAVA_HOME%
    echo         Edit JAVA_HOME at the top of start.cmd.
    pause
    exit /b 1
)
if not exist "%MVN%" (
    echo [ERROR] Maven not found: %MVN%
    echo         Edit MVN at the top of start.cmd.
    pause
    exit /b 1
)
set "JAVA_HOME=%JAVA_HOME%"

echo.
echo ===============================================
echo   Mall app - starting on port %PORT%
echo ===============================================
echo.

rem ---- dependency services (warn only, never block) ----
sc query MySQL 2>nul | "%FINDSTR%" /C:"RUNNING" >nul 2>&1
if errorlevel 1 echo   [WARN] MySQL is not RUNNING - product list will fail to load.

sc query Redis 2>nul | "%FINDSTR%" /C:"RUNNING" >nul 2>&1
if errorlevel 1 echo   [WARN] Redis is not RUNNING - login captcha will return 503.

rem ---- port must be free, otherwise Spring Boot fails with a confusing error ----
set "BUSYLINE="
for /f "tokens=*" %%L in ('netstat -ano ^| "%FINDSTR%" ":%PORT% " ^| "%FINDSTR%" "LISTENING"') do (
    if not defined BUSYLINE set "BUSYLINE=%%L"
)

if defined BUSYLINE (
    echo.
    echo   [ERROR] Port %PORT% is already in use by another program:
    echo          %BUSYLINE%
    echo.
    echo   Free that port, or start on another one:
    echo          start.cmd 8081
    echo.
    pause
    exit /b 1
)

echo   MySQL / Redis checked, port %PORT% is free.
echo.
echo   URL  : http://localhost:%PORT%/
echo   Stop : Ctrl+C here, or run stop.cmd in another window
echo   Log  : this window
echo.
echo -----------------------------------------------
echo.

rem ---- run in the foreground so the log stays visible ----
call "%MVN%" -B spring-boot:run "-Dspring-boot.run.arguments=--server.port=%PORT%"

echo.
echo   App exited. If that was unexpected, check the log above.
pause
