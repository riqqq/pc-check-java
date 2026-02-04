@echo off
set "APP_DIR=%~dp0"
set "JAVA_HOME=%APP_DIR%jdk-17.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "LIBS_DIR=%APP_DIR%libs"
set "SRC_DIR=%APP_DIR%src\main\java"
set "OUT_DIR=%APP_DIR%bin"

echo Using JAVA_HOME=%JAVA_HOME%
if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"

REM Clean bin
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo [1/3] Checking dependencies...
if not exist "%LIBS_DIR%\jna-5.13.0.jar" (
    echo Downloading jna-5.13.0.jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar' -OutFile '%LIBS_DIR%\jna-5.13.0.jar'"
)
if not exist "%LIBS_DIR%\jna-platform-5.13.0.jar" (
    echo Downloading jna-platform-5.13.0.jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.13.0/jna-platform-5.13.0.jar' -OutFile '%LIBS_DIR%\jna-platform-5.13.0.jar'"
)

echo [2/3] Compiling...
dir /s /B "%SRC_DIR%\*.java" > sources.txt
javac -d "%OUT_DIR%" -cp "%LIBS_DIR%\jna-5.13.0.jar;%LIBS_DIR%\jna-platform-5.13.0.jar" @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation Failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Running Application...
echo ======================================================
java -cp "%OUT_DIR%;%LIBS_DIR%\jna-5.13.0.jar;%LIBS_DIR%\jna-platform-5.13.0.jar" com.dmacheese.pccheck.Main
echo ======================================================
pause
