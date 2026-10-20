@echo off
rem ===========================================================================
rem  Mall DB import helper  (Windows batch)
rem
rem  Why does this file exist?
rem    PowerShell does NOT support the "<" redirection operator
rem    (it fails with "The '<' operator is reserved for future use."),
rem    so the import has to be done by cmd.exe.
rem
rem  Usage - pick any one:
rem    1. double-click this file
rem    2. from PowerShell:   cmd /c "sql\import.cmd"
rem    3. from cmd.exe:      sql\import.cmd
rem
rem  NOTE: keep this file ASCII-only and do NOT call "chcp" here.
rem    cmd.exe reads a batch file line by line using the CURRENT code page,
rem    so switching the code page in the middle of the file makes it lose
rem    sync and try to execute garbled text as commands.
rem ===========================================================================

rem Path to the local mysql client. Change this line on another machine;
rem if it does not exist we fall back to whatever "mysql" is on PATH.
set "MYSQL=D:\MySQL Server 5.7\bin\mysql.exe"
if not exist "%MYSQL%" set "MYSQL=mysql"

echo.
echo   SQL file : %~dp0init.sql
echo   Charset  : utf8mb4  (keeps the product emoji intact)
echo   Account  : root  -  you will be asked for the password
echo.

"%MYSQL%" --default-character-set=utf8mb4 -u root -p < "%~dp0init.sql"

echo.
if errorlevel 1 (
    echo   [FAILED] Import did not finish. Check the error above:
    echo            Access denied        -^> wrong root password
    echo            Permission denied    -^> account cannot create db/user
    echo.
    echo   Workaround: comment out section 2 of init.sql ^(CREATE USER/GRANT^)
    echo   and point application.properties at your own MySQL account.
) else (
    echo   [OK] Import finished.
    echo        The self-check above should print: users 2 / products 18 / cart 0
    echo        Next step:  mvn spring-boot:run   then open http://localhost:8080/
)
echo.
pause
