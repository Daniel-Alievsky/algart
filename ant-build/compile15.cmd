@set JDK_VERSION=jdk1.5.0_22
@set JAVA_HOME=%ProgramFiles%\Java\%JDK_VERSION%
@if not "%ProgramFiles(x86)%"=="" set JAVA_HOME=%ProgramFiles(x86)%\Java\%JDK_VERSION%
@call ant compile
