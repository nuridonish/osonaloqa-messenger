@echo off
cd /d "%~dp0server"
:loop
call npx tsx src/index.ts > ..\server.log 2>&1
timeout /t 3 /nobreak >nul
goto loop
