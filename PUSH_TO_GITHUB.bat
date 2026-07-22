@echo off
setlocal
cd /d "%~dp0"

echo =============================================
echo  Hustle Rush - Upload complete project
 echo Repository: piyushcakesportal/hustle-rush-android
 echo =============================================

where git >nul 2>nul
if errorlevel 1 (
  echo.
  echo Git is not installed. Install Git for Windows, then run this file again.
  pause
  exit /b 1
)

if not exist ".git" git init

git config user.name "piyushcakesportal"
git config user.email "piyushcakesportal@users.noreply.github.com"

git remote remove origin >nul 2>nul
git remote add origin https://github.com/piyushcakesportal/hustle-rush-android.git

git add -A
git commit -m "Upload complete Hustle Rush Android project"
git branch -M main

echo.
echo GitHub may open a browser window for login.
git push -u origin main --force

if errorlevel 1 (
  echo.
  echo Upload failed. Read the error shown above.
  pause
  exit /b 1
)

echo.
echo Upload complete. Open GitHub Actions and wait for Build Android.
pause
