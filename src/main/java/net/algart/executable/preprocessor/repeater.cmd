@echo off
if not exist Repeater.java (
    echo repeater.cmd is called from a wrong folder:
    cd
    echo It should be called from the folder containing it.
    goto end
)
set f=
set flags= -trimTrailingWhitespace -warningOnTabs
if %1.==-tempRemove. set flags=%flags% -removeAllIncludes
if %1.==-trim. set flags=%flags% -onlyTrimTrailingWhitespace

set f=%f% "..\..\..\..\..\**\*.java"
set f=%f% "..\..\..\..\..\..\test\**\*.java"
rem set f=%f% "(some-folder)\src\main\java\**\*.java"

if exist ..\..\..\..\..\..\..\target\classes\net\algart\executable\preprocessor\Repeater.class (
    echo Calling Repeater compiled in the target\ folder
    goto calljava
)
if exist Repeater.class goto calljava
if "%JAVA_HOME%"=="" goto simplejavac

echo Calling %JAVA_HOME%\bin\javac -d ..\..\..\.. Repeater.java
"%JAVA_HOME%\bin\javac" -version -d ..\..\..\.. Repeater.java
goto calljava

:simplejavac
rem JAVA_HOME is not set: let's try to find javac in path
echo Calling javac -d ..\..\..\.. Repeater.java
javac -version -d ..\..\..\.. Repeater.java

:calljava
java -cp ..\..\..\..;..\..\..\..\..\..\..\target\classes\ net.algart.executable.preprocessor.Repeater %flags% %f%
if errorlevel 1 pause

:end