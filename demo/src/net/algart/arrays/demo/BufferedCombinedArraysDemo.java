/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.Locale;

/**
 * <p>Demo of using combined arrays: buffered filling and reading.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class BufferedCombinedArraysDemo {
    static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    static class Point {
        int x;
        int y;

        Point() {
        }

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return x + "," + y;
        }
    }

    static class MyCombiner implements CombinedMemoryModel.BufferedCombiner<Point> {
        public Point get(long index, Array[] storage) {
            IntArray a = (IntArray) storage[0];
            return new Point(a.getInt(2 * index), a.getInt(2 * index + 1));
        }

        public void set(long index, Point value, UpdatableArray[] storage) {
            Point pt = value == null ? new Point() : value;
            UpdatableIntArray a = (UpdatableIntArray) storage[0];
            a.setInt(2 * index, pt.x);
            a.setInt(2 * index + 1, pt.y);
        }

        private IntArray lastArray = null;
        private int lastCount = 0;
        private DataIntBuffer buf = null;

        private void createBuffer(int count, Array[] storage) {
            // optimization: use the same buffer when possible
            IntArray a = (IntArray) storage[0];
            if (a != lastArray || count > lastCount) {
                buf = a.buffer(DataBuffer.AccessMode.READ_WRITE, 2 * count);
                lastArray = a;
                lastCount = count;
            }
        }

        public void get(long index, Point[] resultValues, int offset, int count, Array[] storage) {
            createBuffer(count, storage);
            buf.map(2 * index);
            int[] data = buf.data();
            for (int k = 0, from = buf.from(); k < count; k++, offset++) {
                if (resultValues[offset] == null) {
                    resultValues[offset] = new Point(data[from + 2 * k], data[from + 2 * k + 1]);
                } else {
                    // optimization: use the same instance when possible
                    resultValues[offset].x = data[from + 2 * k];
                    resultValues[offset].y = data[from + 2 * k + 1];
                }
            }
        }

        public void set(long index, Point[] values, int offset, int count, UpdatableArray[] storage) {
            createBuffer(count, storage);
            buf.map(2 * index);
            int[] data = buf.data();
            for (int k = 0, from = buf.from(); k < count; k++, offset++) {
                Point pt = values[offset] == null ? new Point() : values[offset];
                data[from + 2 * k] = pt.x;
                data[from + 2 * k + 1] = pt.y;
            }
            buf.force();
        }

        public UpdatableArray[] allocateStorage(long length, boolean unresizable) {
            return new UpdatableArray[]{
                unresizable ? mm.newUnresizableIntArray(2 * length) : mm.newIntArray(2 * length)};
        }

        public int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage) {
            return 2;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: " + BufferedCombinedArraysDemo.class.getName()
                + " arrayLength [numberOfIterations]");
            return;
        }

        System.out.println(Arrays.SystemSettings.globalMemoryModel());
        System.out.println();
        long arrayLength = Long.parseLong(args[0]);
        int numberOfIterations = args.length >= 2 ? Integer.parseInt(args[1]) : 3;

        MemoryModel cmm = CombinedMemoryModel.<Point>getInstance(new MyCombiner());
        UpdatableObjectArray<Point> a = cmm.newUnresizableObjectArray(Point.class, arrayLength);
        System.out.printf(Locale.US, "Array of points created:%n    %s%n", a);

        DataObjectBuffer<Point> buf = a.buffer(DataBuffer.AccessMode.READ_WRITE, 1024);
        // low buffer capacity allows better using CPU cache
        for (int count = 0; count < numberOfIterations; count++) {
            System.out.printf(Locale.US, "%nIteration #%d%n", count + 1);

            long t1 = System.nanoTime();
            for (buf.map(0); buf.hasData(); buf.mapNext()) {
                Point[] points = buf.data();
                int dx = 2 * (int) buf.position(), dy = -(int) buf.position();
                for (int k = buf.from(); k < buf.to(); k++) {
                    points[k].x = dx + 2 * k;
                    points[k].y = dy + count - k;
                }
                buf.force();
            }
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "Array filled (%.5f seconds, %.3f ns/point)%n    %s%n",
                (t2 - t1) * 1e-9, (t2 - t1 + 0.0) / a.length(), Arrays.toString(a, "; ", 80));

            t1 = System.nanoTime();
            long sum1 = 0;
            for (buf.map(0); buf.hasData(); buf.mapNext()) {
                Point[] points = buf.data();
                for (int k = buf.from(); k < buf.to(); k++) {
                    sum1 += points[k].x + points[k].y;
                }
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Array read via buffer().data():  sum of all coordinates = %d "
                + "(%.5f seconds, %.3f ns/point)%n", sum1, (t2 - t1) * 1e-9, (t2 - t1 + 0.0) / a.length());

            t1 = System.nanoTime();
            long sum2 = 0;
            for (long k = 0, n = a.length(); k < n; k++) {
                Point pt = a.get(k);
                sum2 += pt.x + pt.y;
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Array read via getElement():     sum of all coordinates = %d "
                + "(%.5f seconds, %.3f ns/point)%n", sum2, (t2 - t1) * 1e-9, (t2 - t1 + 0.0) / a.length());
            if (sum1 != sum2)
                throw new AssertionError("Bug found!");

            t1 = System.nanoTime();
            long sum3 = 0;
            for (Point pt : Arrays.asList(a)) {
                sum3 += pt.x + pt.y;
            }
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Array read via Arrays.asList():  sum of all coordinates = %d "
                + "(%.5f seconds, %.3f ns/point)%n", sum3, (t2 - t1) * 1e-9, (t2 - t1 + 0.0) / a.length());
            if (sum1 != sum3)
                throw new AssertionError("Bug found!");
        }
        a = null;
        buf = null; // allows garbage collection
        try {
            if (Arrays.gcAndAwaitFinalization(5000))
                System.out.printf("%nFinalization complete%n");
        } catch (InterruptedException ex) {
        }
    }
}
