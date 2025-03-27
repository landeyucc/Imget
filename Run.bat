@echo off
chcp 65001
echo 正在检查环境...

REM 检查Java是否安装
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误：未找到Java，请先安装Java运行环境
    pause
    exit /b 1
)

REM 检查jar文件是否存在
if not exist Imget.jar (
    echo 错误：未找到Imget.jar文件
    pause
    exit /b 1
)

echo 正在启动Imget...
java -jar Imget.jar