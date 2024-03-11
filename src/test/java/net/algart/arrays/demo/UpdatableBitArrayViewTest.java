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

import net.algart.arrays.BufferMemoryModel;
import net.algart.arrays.SimpleMemoryModel;
import net.algart.arrays.UpdatableBitArray;
import net.algart.arrays.UpdatableIntArray;

import java.nio.ByteBuffer;

public class UpdatableBitArrayViewTest {
    public static void main(String[] args) {
        long[] data = new long[10];
        for (int k = 0; k < data.length; k++) {
            data[k] = 278 * k;
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 8);
        bb.asLongBuffer().put(data);
        int length = data.length * 64 + 1;
        UpdatableBitArray bbAsBits = BufferMemoryModel.asUpdatableBitArray(bb, length);
        UpdatableBitArray arrayAsBits = SimpleMemoryModel.asUpdatableBitArray(data, length);
        UpdatableIntArray bbAsInts = BufferMemoryModel.asUpdatableIntArray(bb);
        long[] bits = new long[data.length];
        bbAsBits.getBits(0, bits, 0, length);
        if (!arrayAsBits.equals(bbAsBits)) {
            throw new AssertionError();
        }
    }
}
