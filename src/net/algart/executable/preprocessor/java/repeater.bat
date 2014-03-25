@echo off
set f=
set flags= -trimTrailingWhitespace -warningOnTabs
if %1.==-tempRemove. set flags=%flags% -removeAllIncludes
if %1.==-trim. set flags=%flags% -onlyTrimTrailingWhitespace

set f=%f% "..\..\..\..\..\..\**\*.java"

set trunk=..\..\..\..\..\..\..\..
if not exist %trunk%\simagis.iml set trunk=..\..\..\..\..\..\..\..\simagis
if not exist %trunk%\simagis.iml set trunk=..\..\..\..\..\..\..\..\trunk
if not exist %trunk%\simagis.iml echo Cannot find SIMAGIS trunk; it will not be processed
if not exist %trunk%\simagis.iml goto skiptrunk

if exist "%trunk%\bin\SimagisMatrixDemo\*.*"     set f=%f% "%trunk%\bin\SimagisMatrixDemo\src\**\*.java"
if exist "%trunk%\bin\AlgartImagesP3\*.*"        set f=%f% "%trunk%\bin\AlgartImagesP3\src\**\*.java"
if exist "%trunk%\bin\MovementCommonModel3D\*.*" set f=%f% "%trunk%\bin\MovementCommonModel3D\**\*.java"
if exist "%trunk%\bin\MovementCommonModel3D\*.*" set f=%f% "%trunk%\bin\MovementCommonModel3D\*.html"
if exist "%trunk%\PlanePyramidSourceNDPRead\*.*" set f=%f% "%trunk%\PlanePyramidSourceNDPRead\VisualStudio_NDPRead\**\NDPReadJNI.cpp"
if exist "%trunk%\OlympusVSI\*.*"                set f=%f% "%trunk%\OlympusVSI\src\**\*.java"
:skiptrunk

if exist ..\..\..\..\..\..\classes\net\algart\executable\preprocessor\java\Repeater.class goto calljava
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
java -cp ..\..\..\..\..;..\..\..\..\..\..\classes net.algart.executable.preprocessor.java.Repeater %flags% %f%
if errorlevel 1 pause

:end