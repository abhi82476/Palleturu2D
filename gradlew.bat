@rem Gradle wrapper for Windows
@if "%DEBUG%" == "" @echo off
@rem Set local scope for the variables
setlocal
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

if "%JAVA_HOME%" == "" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%/bin/java
)

set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

%JAVACMD% %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
