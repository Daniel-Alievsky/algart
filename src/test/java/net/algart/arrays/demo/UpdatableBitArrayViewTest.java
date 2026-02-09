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

package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.stream.IntStream;

public class UpdatableBitArrayViewTest {
    public static void main(String[] args) {
        Random rnd = new Random(123);
        for (int test = 0; test < 16; test++) {
            long[] data = IntStream.range(0, 10).mapToLong(k -> 278L * k).toArray();
            ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 8 + 1);
            bb.order(rnd.nextBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            System.out.printf("%n%s%n", bb.order());
            bb.asLongBuffer().put(data);
            IntStream.range(0, bb.limit()).forEach(k -> System.out.printf("%02x ", bb.get(k)));
            System.out.println();
            int length = data.length * 64 - 13;
            UpdatableBitArray bbAsBits = BufferMemoryModel.asUpdatableBitArray(bb, length);
            UpdatableBitArray bbBits = Arrays.BMM.newUnresizableBitArray(length);
            bbBits.setBits(0, data, 0, length);
            UpdatableBitArray arrayAsBits = BitArray.as(data, length);
            System.out.println(bbAsBits);
            System.out.println(bbBits);
            System.out.println(arrayAsBits);

            long[] bits = new long[data.length];
            bbAsBits.getBits(0, bits, 0, length);
            assert arrayAsBits.equals(bbAsBits);
            for (int k = 0; k < length; k++) {
                boolean b = PackedBitArrays.getBit(bits, k);
                assert b == bbAsBits.getBit(k);
                assert b == bbBits.getBit(k);
                bbAsBits.setBit(k, k % 3 == 0);
                bbBits.setBit(k, k % 3 == 0);
                arrayAsBits.setBit(k, k % 3 == 0);
            }
            assert arrayAsBits.equals(bbAsBits);
            assert arrayAsBits.equals(bbBits);
            bb.rewind();
            IntStream.range(0, bb.limit()).forEach(k -> System.out.printf("%02x ", bb.get(k)));
            System.out.println();
        }
    }
}
