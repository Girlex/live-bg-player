@echo off
chcp 65001 >nul
echo ========================================
echo   Git 版本切换工具
echo ========================================
echo.

:menu
echo 请选择操作：
echo 1. 查看所有版本标签
echo 2. 查看所有提交历史
echo 3. 查看所有分支
echo 4. 切换到指定标签版本
echo 5. 切换到指定 commit
echo 6. 切换回最新 master
echo 7. 退出
echo.
set /p choice=请输入选项 (1-7): 

if "%choice%"=="1" goto show_tags
if "%choice%"=="2" goto show_commits
if "%choice%"=="3" goto show_branches
if "%choice%"=="4" goto checkout_tag
if "%choice%"=="5" goto checkout_commit
if "%choice%"=="6" goto checkout_master
if "%choice%"=="7" goto end
goto menu

:show_tags
echo.
echo === 所有版本标签 ===
git tag -n
echo.
pause
goto menu

:show_commits
echo.
echo === 最近 20 条提交记录 ===
git log --oneline -20
echo.
pause
goto menu

:show_branches
echo.
echo === 所有分支 ===
git branch -a
echo.
pause
goto menu

:checkout_tag
echo.
set /p tag_name=请输入要切换的标签名称 (例如 v1.0-stable): 
git checkout %tag_name%
if %errorlevel% equ 0 (
    echo.
    echo ✓ 成功切换到版本: %tag_name%
    echo ⚠ 注意: 现在处于 "detached HEAD" 状态
    echo    如需修改代码，请创建新分支: git checkout -b new-branch-name
) else (
    echo.
    echo ✗ 切换失败，请检查标签名称是否正确
)
echo.
pause
goto menu

:checkout_commit
echo.
set /p commit_hash=请输入 commit hash (例如 46e8042): 
git checkout %commit_hash%
if %errorlevel% equ 0 (
    echo.
    echo ✓ 成功切换到 commit: %commit_hash%
    echo ⚠ 注意: 现在处于 "detached HEAD" 状态
) else (
    echo.
    echo ✗ 切换失败，请检查 commit hash 是否正确
)
echo.
pause
goto menu

:checkout_master
echo.
git checkout master
git pull origin master
if %errorlevel% equ 0 (
    echo.
    echo ✓ 已切换到最新 master 分支并更新
) else (
    echo.
    echo ✗ 切换失败
)
echo.
pause
goto menu

:end
echo.
echo 感谢使用！
pause
