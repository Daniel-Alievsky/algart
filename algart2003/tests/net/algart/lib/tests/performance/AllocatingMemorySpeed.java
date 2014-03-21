/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib.tests.performance;

import net.algart.lib.*;

public class AllocatingMemorySpeed {
    public static void main(String[] argv) {
        Out.setPrintDelay(200);
        if (argv.length < 3) {
            Out.printlnFlush("Usage: AllocatingMemorySpeed IterationCount BlockSize BlockCount");
            return;
        }
        int iterationCount = Integer.parseInt(argv[0]);
        int blockSize = Integer.parseInt(argv[1]);
        int blockCount = Integer.parseInt(argv[2]);
        byte[][] b = new byte[blockCount][];
        Out.println(Memory.info());
        for (int n = 1; n <= iterationCount; n++) {
            long t1 = Timing.timens();
            for (int k = 0; k < blockCount; k++) {
                b[k] = new byte[blockSize];
            }
            long t2 = Timing.timens();
            for (int k = 0; k < blockCount; k++) {
                for (int i = 0; i < blockSize; i++)
                    b[k][i] = (byte)i;
            }
            long t3 = Timing.timens();
            Out.println(n + ": " + Out.dec((t2-t1)/1000.0,2) + " mcs allocating ("
                + Out.dec((t2-t1+0.)/blockCount/blockSize,2) + " ns/byte), "
                + Out.dec((t3-t2)/1000.0,3) + " mcs filling ("
                + Out.dec((t3-t2+0.)/blockCount/blockSize,2) + " ns/byte)   "
                + Memory.info());
        }

    }
}