@echo off
set f=
set flags= -trimTrailingWhitespace -warningOnTabs
if %1.==-tempRemove. set flags=%flags% -removeAllIncludes
if %1.==-trim. set flags=%flags% -onlyTrimTrailingWhitespace

set f=%f% "..\..\..\..\..\..\**\*.java"

rem set trunk=..\..\..\..\..\..\..\..
rem if not exist %trunk%\simagis.iml set trunk=..\..\..\..\..\..\..\..\simagis
rem if not exist %trunk%\simagis.iml set trunk=..\..\..\..\..\..\..\..\trunk
rem if not exist %trunk%\simagis.iml echo Cannot find SIMAGIS trunk; it will not be processed
rem if not exist %trunk%\simagis.iml goto skiptrunk

rem if exist "%trunk%\bin\SimagisMatrixDemo\*.*"     set f=%f% "%trunk%\bin\SimagisMatrixDemo\src\**\*.java"
rem if exist "%trunk%\bin\AlgartImagesP3\*.*"        set f=%f% "%trunk%\bin\AlgartImagesP3\src\**\*.java"
rem if exist "%trunk%\bin\MovementCommonModel3D\*.*" set f=%f% "%trunk%\bin\MovementCommonModel3D\**\*.java"
rem if exist "%trunk%\bin\MovementCommonModel3D\*.*" set f=%f% "%trunk%\bin\MovementCommonModel3D\*.html"
rem if exist "%trunk%\PlanePyramidSourceNDPRead\*.*" set f=%f% "%trunk%\PlanePyramidSourceNDPRead\VisualStudio_NDPRead\**\NDPReadJNI.cpp"
rem if exist "%trunk%\OlympusVSI\*.*"                set f=%f% "%trunk%\OlympusVSI\src\**\*.java"
rem :skiptrunk

if exist ..\..\..\..\..\..\..\..\target\classes\net\algart\executable\preprocessor\java\Repeater.class goto calljava
if exist Repeater.class goto calljava
if "%JAVA_HOME%"=="" goto simplejavac

echo Calling %JAVA_HOME%\bin\javac -d ..\..\..\..\.. Repeater.java
"%JAVA_HOME%\bin\javac" -version -d ..\..\..\..\.. Repeater.java
goto calljava

:simplejavac
rem JAVA_HOME is not set: let's try to find javac in path
echo Calling javac -d ..\..\..\..\.. Repeater.java
javac -version -d ..\..\..\..\.. Repeater.java

:calljava
java -cp ..\..\..\..\..;..\..\..\..\..\..\..\..\target\classes\ net.algart.executable.preprocessor.java.Repeater %flags% %f%
if errorlevel 1 pause

:end