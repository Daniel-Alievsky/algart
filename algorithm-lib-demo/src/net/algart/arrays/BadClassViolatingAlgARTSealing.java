package net.algart.arrays;

/**
 * <p>Attempt to violate package protection of net.algart.arrays package.
 * Must not be executed if the source jar-file is correctly sealed.</p>
 *
 * <p>Example of calling this method that should be rejected:</p>
 *
 * <pre>
 * "%JAVA_HOME%\bin\java" -cp src;algart.jar net.algart.arrays.BadClassViolatingAlgARTSealing
 * </pre>
 *
 * <p>Here "src" is a directory where this class was compiled, and "algart.jar" is a correctly sealed jar-file
 * with basic AlgART libraries.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class BadClassViolatingAlgARTSealing {
    public static void test() {
        BitArray a = SimpleMemoryModel.getInstance().newUnresizableBitArray(100);
        long offset = 0;
        // offset = Arrays.longJavaArrayOffsetInternal(a); // violation of package protection
        //     If you will uncomment the violation operator above, this class will still be compiled;
        //     but you are not able to execute this class even with commented violation operator with correct jar-file.
        System.out.println("Array: " + a);
        System.out.println("Internal offset " + offset);
    }

    public static void main(String[] args) {
        test();
    }
}
