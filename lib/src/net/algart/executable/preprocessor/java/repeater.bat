@echo off
set f=
set flags= -trimTrailingWhitespace -warningOnTabs
if %1.==-tempRemove. set flags=%flags% -removeAllIncludes
if %1.==-trim. set flags=%flags% -onlyTrimTrailingWhitespace

set f=%f% "..\..\..\..\..\..\..\lib\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\demo\src\net\algart\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\SimagisMatrixDemo\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\AlgartImagesP3\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\algart-extensions\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\algart\src\net\algart\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\src\com\siams\image2d\compatibility\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\src-immutable\net\algart\immutable\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\SimagisRecognitionOfParallels\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\MovementCommonModel3D\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\bin\MovementCommonModel3D\*.html"
set f=%f% "..\..\..\..\..\..\..\..\trunk\ImageJ\ImageJP3\src\com\simagis\ij\p3\specific\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\Ilastik\IlastikP3\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\SimagisLive\SimagisLiveLib\src\com\simagis\live\io\tms\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\PlanePyramidSourceNDPRead\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\PlanePyramidSourceNDPRead\VisualStudio_NDPRead\**\NDPReadJNI.cpp"
set f=%f% "..\..\..\..\..\..\..\..\trunk\OlympusVSI\src\**\*.java"
set f=%f% "..\..\..\..\..\..\..\..\trunk\PlanePyramidSourceOlympus\src\**\*.java"

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