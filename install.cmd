@echo off
chcp 65001 >nul
cd /d "%~dp0"
py -3 -m pip install -r requirements.txt
if errorlevel 1 goto error
py -3 -m playwright install chromium
if errorlevel 1 goto error
echo Installation complete.
pause
exit /b 0
:error
echo Installation failed.
pause
exit /b 1
