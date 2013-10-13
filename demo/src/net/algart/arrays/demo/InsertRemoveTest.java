package net.algart.arrays.demo;

import java.util.Random;
import net.algart.arrays.*;

/**
 * <p>Test for inserting and removing AlgART array elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class InsertRemoveTest {
    private MutableCharArray a;
    private MutableCharArray aWork;
    private StringBuilder sb;
    private StringBuilder sbWork;
    boolean printStrings = false;

    private static String dup(int count, char c) {
        char[] ca = new char[count];
        for (int k = 0; k < count; k++)
            ca[k] = c;
        return new String(ca);
    }

    private InsertRemoveTest(int len) {
        a = Arrays.SystemSettings.globalMemoryModel().newEmptyCharArray();
        for (int k = 0; k < len; k++)
            a.pushChar((char)('A' + (k & 31)));
        aWork = Arrays.SystemSettings.globalMemoryModel().newCharArray(a.length());
        sb = new StringBuilder(Arrays.toString(a));
        sbWork = new StringBuilder(); sbWork.setLength(len);
    }

    private void testMethods(long position, long count) {
        if (printStrings)
            System.out.println("Original:               " + Arrays.toString(a, "", 60)
                + "(" + a.length() + " characters)");

        aWork.length(a.length());
        aWork.copy(a);
        sbWork.setLength(sb.length());
        sbWork.replace(0, sbWork.length(), sb.toString());
        try {
            Arrays.insertChar(aWork, position, '_');
        } catch (Exception ex) {
            System.out.println("Exception caught in Arrays.insertChar");
            System.out.println(ex);
        }
        try {
            sbWork.insert((int)position, '_');
        } catch (Exception ex) {
            System.out.println("Exception caught in StringBuilder.insert");
            System.out.println(ex);
        }
        if (printStrings)
            System.out.println("After insertChar:       " + Arrays.toString(aWork, "", 60)
                + "(" + aWork.length() + " characters)");
        if (!Arrays.toString(aWork).equals(sbWork.toString()))
            throw new AssertionError("Bug in insertChar! position = " + position + ", count = " + count);

        aWork.length(a.length());
        aWork.copy(a);
        sbWork.setLength(sb.length());
        sbWork.replace(0, sbWork.length(), sb.toString());
        try {
            Arrays.insertEmptyRange(aWork, position, count);
            aWork.subArr(position, count).copy(Arrays.nCharCopies(count, '7'));
        } catch (Exception ex) {
            System.err.println("Exception caught in Arrays.insertEmptyRange");
            System.err.println(ex);
        }
        try {
            sbWork.insert((int)position, dup((int)count, '7'));
        } catch (Exception ex) {
            System.err.println("Exception caught in StringBuilder.insert");
            System.err.println(ex);
        }
        if (printStrings)
            System.out.println("After inserting array: " + Arrays.toString(aWork, "", 60)
                + "(" + aWork.length() + " characters)");
        if (!Arrays.toString(aWork).equals(sbWork.toString()))
            throw new AssertionError("Bug in insertEmptyRange / copy / nCharCopies! position = "
                + position + ", count = " + count);

        aWork.length(a.length());
        aWork.copy(a);
        sbWork.setLength(sb.length());
        sbWork.replace(0, sbWork.length(), sb.toString());
        try {
            Arrays.removeRange(aWork, position, count);
        } catch (Exception ex) {
            System.err.println("Exception caught in Arrays.removeRange");
            System.err.println(ex);
        }
        try {
            sbWork.delete((int)position, (int)(position + count));
        } catch (Exception ex) {
            System.err.println("Exception caught in StringBuilder.delete");
            System.err.println(ex);
        }
        if (printStrings)
            System.out.println("After removeRange:      " + Arrays.toString(aWork, "", 60)
                + "(" + aWork.length() + " characters)");
        if (!Arrays.toString(aWork).equals(sbWork.toString()))
            throw new AssertionError("Bug in removeRange! position = " + position + ", count = " + count);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("    " + InsertRemoveTest.class.getName() + " arrayLength numberOfTests");
            System.out.println("or");
            System.out.println("    " + InsertRemoveTest.class.getName() + " arrayLength position count");
            return;
        }
        DemoUtils.initializeClass();
        System.out.println(Arrays.SystemSettings.globalMemoryModel().toString());
        System.out.println();

        int len = Integer.parseInt(args[0]);
        InsertRemoveTest test = new InsertRemoveTest(len);
        if (args.length >= 3) {
            long position = Long.parseLong(args[1]);
            long count = Long.parseLong(args[2]);
            test.printStrings = true;
            test.testMethods(position, count);
        } else {
            Random rnd = new Random();
            int numberOfTests = Integer.parseInt(args[1]);

            for (int testCount = 0; testCount < numberOfTests; testCount++) {
                int position = rnd.nextInt(len + 1);
                int count = rnd.nextInt(len + 1 - position);
                test.testMethods(position, count);
                DemoUtils.showProgress(testCount, numberOfTests);
            }
            System.out.println("All O'k: testing time " + (System.currentTimeMillis() - DemoUtils.tStart) + " ms");
        }
        test = null; // allows garbage collection
        DemoUtils.fullGC();
    }
}
