@echo off
set "APP_DIR=%~dp0"
set "JAVA_HOME=%APP_DIR%jdk-17.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "DIST_DIR=%APP_DIR%dist"
set "LIBS_DIR=%APP_DIR%libs"
set "SRC_DIR=%APP_DIR%src\main\java"

echo [1/4] Preparing 'dist' directory...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"
mkdir "%DIST_DIR%\libs"

echo [2/4] Copying dependencies...
copy /Y "%LIBS_DIR%\*.jar" "%DIST_DIR%\libs\" > nul

echo [3/4] Compiling and Packaging JAR...
if not exist "bin" mkdir "bin"
dir /s /B "%SRC_DIR%\*.java" > sources.txt
echo [INFO] Compiling for Java 8 compatibility...
javac --release 8 -d bin -cp "%LIBS_DIR%\*" @sources.txt

echo Warning: Packing minimal JAR (Main-Class manifest)...
echo Main-Class: com.dmacheese.pccheck.Main > manifest.txt
echo Class-Path: libs/jna-5.13.0.jar libs/jna-platform-5.13.0.jar >> manifest.txt
jar cvfm "%DIST_DIR%\pc_check.jar" manifest.txt -C bin .
del manifest.txt sources.txt
rmdir /s /q bin

echo [4/4] Creating Run Script (User JRE)...
set "RUN_BAT=%DIST_DIR%\run.bat"
echo @echo off > "%RUN_BAT%"
echo echo PC Check - Distribution Mode >> "%RUN_BAT%"
echo echo. >> "%RUN_BAT%"
echo echo [INFO] Checking Java Version... >> "%RUN_BAT%"
echo java -version >> "%RUN_BAT%"
echo echo. >> "%RUN_BAT%"
echo echo [INFO] Starting Application... >> "%RUN_BAT%"
echo echo --------------------------------------------------- >> "%RUN_BAT%"
echo java -jar pc_check.jar >> "%RUN_BAT%"
echo echo --------------------------------------------------- >> "%RUN_BAT%"
echo echo. >> "%RUN_BAT%"
echo if %%ERRORLEVEL%% NEQ 0 ( >> "%RUN_BAT%"
echo     echo [ERROR] Application exited with error code %%ERRORLEVEL%%. >> "%RUN_BAT%"
echo     echo This often means Java is outdated (Need Java 17+). >> "%RUN_BAT%"
echo ) else ( >> "%RUN_BAT%"
echo     echo [INFO] Application finished. >> "%RUN_BAT%"
echo ) >> "%RUN_BAT%"
echo echo. >> "%RUN_BAT%"
echo pause >> "%RUN_BAT%"

echo.
echo ========================================================
echo SUCCESS: Distribution created at C:\pc_check_java\dist
echo You can copy the 'dist' folder to any PC with Java.
echo ========================================================
pause
