/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Arrays;
import net.algart.arrays.MemoryModel;
import net.algart.arrays.UpdatablePArray;

/**
 * <p>Simple test for behavior of copy-on-write arrays while attempt to modify it.</p>
 *
 * @author Daniel Alievsky
 */
public class ModifiyingCopyOnWriteTest {
    public static void main(String args[]) {
        final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        final UpdatablePArray source = mm.newByteArray(25);
        System.out.println("Creating " + source);
        for (int k = 0; k < source.length(); k++) {
            source.setInt(k, k);
        }
        final UpdatablePArray copyOnWrite = (UpdatablePArray) source.asCopyOnNextWrite();
        System.out.println("Copy-on-write:                                 " + Arrays.toString(copyOnWrite, ",", 255));

        // Note: the following attempts to change copy-on-write array via its subarray will not be successful
        copyOnWrite.subArr(0, 25).swap(5, 0, 5);
        System.out.println("Copy-on-write after changing its subarray (1): " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.subArr(0, 25).copy(5, 0, 5);
        System.out.println("Copy-on-write after changing its subarray (2): " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.subArr(0, 25).copy(Arrays.SMM.newByteArray(25));
        System.out.println("Copy-on-write after changing its subarray (3): " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.subArr(0, 25).copy(mm.newByteArray(25));
        System.out.println("Copy-on-write after changing its subarray (4): " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.subArr(0, 25).fill(40);
        System.out.println("Copy-on-write after changing its subarray (5): " + Arrays.toString(copyOnWrite, ",", 255));

        copyOnWrite.swap(5, 0, 5);
        System.out.println("Copy-on-write after changing it directly (1):  " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.copy(5, 0, 5);
        System.out.println("Copy-on-write after changing it directly (2):  " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.copy(Arrays.SMM.newByteArray(25));
        System.out.println("Copy-on-write after changing it directly (3):  " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.copy(mm.newByteArray(25));
        System.out.println("Copy-on-write after changing it directly (4):  " + Arrays.toString(copyOnWrite, ",", 255));
        copyOnWrite.fill(40);
        System.out.println("Copy-on-write after changing it directly (5):  " + Arrays.toString(copyOnWrite, ",", 255));
        System.out.println("Source array:                                  " + Arrays.toString(source, ",", 255));
    }
}
