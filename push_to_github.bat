@echo off
echo ========================================
echo   推送代码到 GitHub
echo ========================================
echo.
echo 当前分支: 
git branch
echo.
echo 远程仓库: 
git remote -v
echo.
echo 开始推送...
echo.

git push -u origin master --tags

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   ✓ 推送成功！
    echo ========================================
    echo.
    echo 仓库地址: https://github.com/girlex/live-bg-player
    echo.
) else (
    echo.
    echo ========================================
    echo   ✗ 推送失败
    echo ========================================
    echo.
    echo 可能的原因:
    echo 1. GitHub 仓库不存在，请先在 GitHub 创建仓库
    echo 2. 网络连接问题
    echo 3. 需要身份验证（请使用 Personal Access Token）
    echo.
)

pause
