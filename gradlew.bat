@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows (PvP Domain)
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Prefer Java 21 runtime for Minecraft 1.21 & Fabric Loom compatibility
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\jbr"
    goto findJavaFromJavaHome
)

@rem Resolve Java executable
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto fail

:execute
@rem Setup classpath for the wrapper jar
set WRAPPER_JAR="%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"

if not exist %WRAPPER_JAR% (
    echo [ERROR] gradle-wrapper.jar not found at %WRAPPER_JAR%
    goto fail
)

"%JAVA_EXE%" -Dorg.gradle.appname=%APP_BASE_NAME% -classpath %WRAPPER_JAR% org.gradle.wrapper.GradleWrapperMain %*
set GRADLE_EXIT_CODE=%ERRORLEVEL%
goto mainEnd

:fail
set GRADLE_EXIT_CODE=1

:mainEnd
if "%OS%"=="Windows_NT" endlocal
exit /b %GRADLE_EXIT_CODE%
