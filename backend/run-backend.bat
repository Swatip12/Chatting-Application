@echo off
echo Starting Chat Backend...

REM Try to find Java installation
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.9
if not exist "%JAVA_HOME%\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Java\jdk-21
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Java\jdk-17
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java not found. Please install Java or set JAVA_HOME manually.
    pause
    exit /b 1
)

echo Using Java from: %JAVA_HOME%
echo.

REM Run Maven wrapper
mvnw.cmd spring-boot:run

pause