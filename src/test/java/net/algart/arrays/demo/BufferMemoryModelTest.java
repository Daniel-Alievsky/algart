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

import net.algart.arrays.BufferMemoryModel;
import net.algart.arrays.UpdatableIntArray;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * <p>Demo of using {@link BufferMemoryModel}.</p>
 *
 * @author Daniel Alievsky
 */
public class BufferMemoryModelTest {
    public static void main(String[] args) {
        int startArgIndex = 0;
        boolean direct = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-direct")) {
            direct = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + BufferMemoryModelTest.class.getName()
                + " [-direct] arrayLength numberOfIterations");
            return;
        }
        int n = Integer.parseInt(args[startArgIndex]);
        int numberOfIterations = Integer.parseInt(args[startArgIndex + 1]);
        ByteBuffer bb = direct ?
            ByteBuffer.allocateDirect(4 * n) :
            ByteBuffer.allocate(4 * n);
        IntBuffer ib = bb.asIntBuffer();
        bb.position(bb.limit() / 2); // must work even in this case
        UpdatableIntArray ia = BufferMemoryModel.asUpdatableIntArray(bb);
        IntBuffer sib = n >= 10 ? ib.position(10).slice() : ib;
        UpdatableIntArray sia = n >= 10 ? ia.subArray(10, n) : ia;
        for (int iteration = 1; iteration <= numberOfIterations; iteration++) {
            System.out.println("Iteration #" + iteration + "/" + numberOfIterations);
            long t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                ib.put(k, k);
            }
            long t2 = System.nanoTime();
            sib.rewind();
            long sum1 = 0;
            for (int k = 0, len = sib.limit(); k < len; k++) {
                sum1 += sib.get();
            }
            long t3 = System.nanoTime();
            long sum2 = 0;
            for (int k = 0, len = (int)sia.length(); k < len; k++) {
                sum2 += sia.getInt(k);
            }
            long t4 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                ia.setInt(k, k);
            }
            long t5 = System.nanoTime();
            System.out.printf(Locale.US, "Filling IntBuffer:         %.5f ms, %.3f ns/element (%d elements)%n",
                (t2 - t1) * 1e-6, (double)(t2 - t1) / ib.limit(), ib.limit());
            System.out.printf(Locale.US, "Sum of IntBuffer:          %.5f ms, %.3f ns/element (%d elements), "
                + "sum = %d%n",
                (t3 - t2) * 1e-6, (double)(t3 - t2) / sib.limit(), sib.limit(), sum1);
            System.out.printf(Locale.US, "Sum of UpdatableIntArray:  %.5f ms, %.3f ns/element (%d elements), "
                + "sum = %d%n",
                (t4 - t3) * 1e-6, (double)(t4 - t3) / sia.length(), sia.length(), sum2);
            System.out.printf(Locale.US, "Filling UpdatableIntArray: %.5f ms, %.3f ns/element (%d elements)%n",
                (t5 - t4) * 1e-6, (double)(t5 - t4) / ia.length(), ia.length());
            if (sum1 != sum2)
                throw new AssertionError("A bug is found: invalid sum");
            System.out.println();
        }
    }
}
