/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.util.List;
import java.util.Optional;

public class ArrayQuickAccessTest {
    private static void checkArray(Array array) {
        final Optional<Object> quick = array.quick();
        final Object ja = array instanceof DirectAccessible da && da.hasJavaArray() ? da.javaArray() : null;
        final int offset = array instanceof DirectAccessible da && da.hasJavaArray() ? da.javaArrayOffset() : -1;
        if (ja == null) {
            System.out.printf("%s%n    usual%n", array);
        } else {
            System.out.printf("%s%n    direct-accessible: %s, offset %d%n", array, ja, offset);
        }
        if (quick.isPresent() != (ja != null && offset == 0)) {
            throw new AssertionError("quick()/DirectAccessible state mismatch: " + quick);
        }
        if (quick.isPresent() && quick.get() != ja) {
            throw new AssertionError("quick()/DirectAccessible content mismatch: " + quick);
        }
    }

    private static void check(Array array) {
        System.out.printf("%nTesting %s and its sub-arrays...%n", array);
        checkArray(array);
        checkArray(array.subArray(0, array.length()));
        checkArray(array.subArray(10, array.length()));
        checkArray(array.subArr(1, 0));
    }

    public static void main(String[] args) {
        for (MemoryModel mm : List.of(Arrays.SMM, BufferMemoryModel.getInstance(), LargeMemoryModel.getInstance())) {
            check(mm.newUnresizableBitArray(1000));
            check(mm.newUnresizableFloatArray(1000));
            check(mm.newIntArray(1000));
            if (mm.isElementTypeSupported(Object.class)) {
                check(mm.newObjectArray(String.class, 100));
            }
        }
        check(Arrays.nIntCopies(1000, 1));
        check(Arrays.nBitCopies(100, false));
        check(Arrays.nObjectCopies(10033, null));
    }
}
