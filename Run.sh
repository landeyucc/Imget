#!/bin/bash

echo "正在检查环境..."

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "错误：未找到Java，请先安装Java运行环境"
    read -p "按回车键退出..."
    exit 1
fi

# 检查jar文件是否存在
if [ ! -f "Imget.jar" ]; then
    echo "错误：未找到Imget.jar文件"
    read -p "按回车键退出..."
    exit 1
fi

# 检查图形界面环境
if [ -z "$DISPLAY" ]; then
    echo "警告：未检测到图形界面环境"
    echo "本程序需要图形界面才能运行"
    echo "请确保："
    echo "1. 已安装图形界面（如X11、Wayland等）"
    echo "2. 已安装Java图形库"
    echo "3. 如果使用SSH，请确保启用了X11转发（使用 -X 参数）"
    read -p "是否继续运行？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "正在启动Imget..."
java -jar Imget.jar