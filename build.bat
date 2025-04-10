@echo off
chcp 65001
REM 创建临时目录
if not exist temp\classes (
    mkdir temp\classes
)

REM 编译所有Java文件到temp/classes目录
javac --release 17 -d temp\classes -sourcepath src -cp lib\json-20231013.jar src\*.java src\model\*.java src\ui\*.java src\utils\*.java

REM 复制resources文件夹到temp/classes
xcopy /E /I /Y src\resources temp\classes\src\resources

REM 打包生成JAR文件
jar cfm Imget.jar MANIFEST.MF -C temp\classes .

REM 清理临时文件
rmdir /S /Q temp

echo 编译完成，生成Imget.jar
pauses