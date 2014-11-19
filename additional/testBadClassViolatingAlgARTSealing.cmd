call "%JAVA_HOME%\bin\javac" -cp ../target/ant-jars/algart.jar -d ../target/test-classes ../src/test/java/net/algart/arrays/BadClassViolatingAlgARTSealing.java
rem - It is compiled
call "%JAVA_HOME%\bin\java" -cp ../target/ant-jars/algart.jar;../target/test-classes net.algart.arrays.BadClassViolatingAlgARTSealing
rem - But it leads to exception "sealing violation"
