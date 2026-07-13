@echo off
REM ############################################################################
REM Build Script for Ticket Tracker Java Application (Windows)
REM ############################################################################

setlocal enabledelayedexpansion

echo ===========================================================================
echo Ticket Tracker - Java Build Script
echo ===========================================================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed. Please install Maven 3.6+ and try again.
    exit /b 1
)

echo [INFO] Maven found
call mvn --version
echo.

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed. Please install JDK 8 and try again.
    exit /b 1
)

echo [INFO] Java found
call java -version
echo.

REM Parse command line arguments
set SKIP_TESTS=false
set CLEAN=true

:parse_args
if "%~1"=="" goto end_parse_args
if "%~1"=="--skip-tests" (
    set SKIP_TESTS=true
    shift
    goto parse_args
)
if "%~1"=="--no-clean" (
    set CLEAN=false
    shift
    goto parse_args
)
echo [ERROR] Unknown option: %~1
echo Usage: %0 [--skip-tests] [--no-clean]
exit /b 1

:end_parse_args

REM Clean project
if "%CLEAN%"=="true" (
    echo [INFO] Cleaning project...
    call mvn clean
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Clean failed
        exit /b 1
    )
    echo.
)

REM Build project
echo [INFO] Building project...
if "%SKIP_TESTS%"=="true" (
    echo [WARN] Skipping tests
    call mvn package -DskipTests
) else (
    call mvn package
)

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed
    exit /b 1
)

echo.

REM Check if WAR was created
set WAR_FILE=target\ticket-tracker.war
if exist "%WAR_FILE%" (
    echo [INFO] Build successful!
    echo.
    echo [INFO] WAR file created: %WAR_FILE%
    echo [INFO] WAR file size:
    dir "%WAR_FILE%" | find "ticket-tracker.war"
    echo.
    echo [INFO] Next steps:
    echo   1. Deploy WAR to Tomcat: copy %WAR_FILE% %%TOMCAT_HOME%%\webapps\
    echo   2. Start Tomcat: %%TOMCAT_HOME%%\bin\startup.bat
    echo   3. Access application: http://localhost:8080/ticket-tracker
    echo.
) else (
    echo [ERROR] Build failed! WAR file not found.
    exit /b 1
)

echo ===========================================================================
endlocal
