@echo off
set DIR=%~dp0
if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%\bin\java
)
"%JAVACMD%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
