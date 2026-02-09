/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.arrays;

/**
 * <p>Attempt to violate package protection of net.algart.arrays package.
 * Must not be executed if the source jar-file is correctly sealed.</p>
 *
 * <p>Example of calling this method that should be rejected:</p>
 *
 * <pre>
 * "%JAVA_HOME%\bin\java" -cp target/test-classes;algart.jar net.algart.arrays.BadClassViolatingAlgARTSealing
 * </pre>
 *
 * <p>Here "target/test-classes" is a directory where this class was compiled, and "algart.jar"
 * is a correctly sealed jar-file with basic AlgART libraries.</p>
 *
 * @author Daniel Alievsky
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
