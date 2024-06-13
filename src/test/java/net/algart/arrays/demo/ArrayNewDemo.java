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

public class ArrayNewDemo {
    public static void main(String[] args) {
        UpdatableArray a1 = Array.newArray(String.class, 1000);
        System.out.println(a1);
        UpdatablePArray a2 = PArray.newArray(byte.class, 900);
        System.out.println(a2);
        UpdatablePArray a3 = PArray.newArray(Arrays.BMM, byte.class, 900);
        System.out.println(a3);
        if (!a2.equals(a3)) {
            throw new AssertionError();
        }
        System.out.println(Arrays.clone(a3));
        UpdatablePArray a4 = PArray.newArray(boolean.class, 1000);
        System.out.println(a4);
        // PNumberArray.newArray(Arrays.BMM, boolean.class, 1000);
        UpdatablePNumberArray a5 = PNumberArray.newArray(float.class, 1000);
        System.out.println(a5);
        UpdatableBitArray a6 = BitArray.newArray(10000);
        System.out.println(a6);
        UpdatableBitArray a7 = BitArray.newArray(Arrays.BMM, 10000);
        System.out.println(a7);
        MutableArray a8 = MutableArray.newArray(Arrays.BMM, short.class);
        System.out.println(a8);
        // MutableArray a9 = MutablePArray.newEmpty(String.class);
        MutablePArray a9 = MutablePArray.newArray(char.class);
        System.out.println(a9);
        MutablePNumberArray a10 = MutablePNumberArray.newArray(float.class);
        System.out.println(a10);
        MutableBitArray a11 = MutableBitArray.newArray(Arrays.BMM);
        System.out.println(a11);
        MutableDoubleArray a12 = MutableDoubleArray.newArray();
        System.out.println(a12);
    }
}
