@echo off
echo Calling javac -d ..\..\..\..\.. Repeater.java
"%JAVA_HOME%\bin\javac" -d ..\..\..\..\.. Repeater.java
copy repeaterTest.src.txt repeaterTest.txt 
"%JAVA_HOME%\bin\java" -cp ..\..\..\..\.. net.algart.executable.preprocessor.java.Repeater -trimTrailingWhitespace -warningOnTabs %1 repeaterTest.txt
if errorlevel 1 pause