/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.algart.arrays.*;
import net.algart.math.functions.LinearFunc;

/**
 * <p>Demo of using combined arrays: filling and sorting.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class CombinedArraysDemo {
    static class Circle implements Comparable<Circle> {
        int x;
        int y;
        int r;
        Circle() {
        }

        Circle(int x, int y, int r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }

        public int compareTo(Circle o) {
            return r - o.r;
        }

        public String toString() {
            return x + ", " + y + ", r=" + r;
        }

        public int hashCode() {
            return x ^ y ^ r;
        }

        // must be implemented for correct work of equals method in NCopies
        public boolean equals(Object obj) {
            if (!(obj instanceof Circle))
                return false;
            Circle o = (Circle)obj;
            return o.x == x && o.y == y && o.r == r;
        }
    }

    static class MyCombinerMulti implements CombinedMemoryModel.CombinerInPlace<Circle> {
        private final MemoryModel mm;

        public MyCombinerMulti(MemoryModel mm) {
            this.mm = mm;
        }

        public Circle allocateElement() {
            return new Circle();
        }

        public void getInPlace(long index, Circle resultValue, Array[] storage) {
            resultValue.x = ((IntArray)storage[0]).getInt(index);
            resultValue.y = ((IntArray)storage[1]).getInt(index);
            resultValue.r = ((IntArray)storage[2]).getInt(index);
        }

        public Circle get(long index, Array[] storage) {
            Circle circ = new Circle();
            circ.x = ((IntArray)storage[0]).getInt(index);
            circ.y = ((IntArray)storage[1]).getInt(index);
            circ.r = ((IntArray)storage[2]).getInt(index);
            return circ;
        }

        public void set(long index, Circle value, UpdatableArray[] storage) {
            Circle circ = value == null ? new Circle() : value;
            ((UpdatableIntArray)storage[0]).setInt(index, circ.x);
            ((UpdatableIntArray)storage[1]).setInt(index, circ.y);
            ((UpdatableIntArray)storage[2]).setInt(index, circ.r);
        }

        public UpdatableArray[] allocateStorage(long length, boolean unresizable) {
            return new UpdatableArray[] {
                unresizable ? mm.newUnresizableIntArray(length) : mm.newIntArray(length),
                unresizable ? mm.newUnresizableIntArray(length) : mm.newIntArray(length),
                unresizable ? mm.newUnresizableIntArray(length) : mm.newIntArray(length)
            };
        }

        public int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage) {
            return 1;
        }
    }

    static class MyCombinerSingle implements CombinedMemoryModel.CombinerInPlace<Circle> {
        final MemoryModel mm;

        public MyCombinerSingle(MemoryModel mm) {
            this.mm = mm;
        }

        public Circle allocateElement() {
            return new Circle();
        }

        public void getInPlace(long index, Circle resultValue, Array[] storage) {
            resultValue.x = ((IntArray)storage[0]).getInt(3 * index);
            resultValue.y = ((IntArray)storage[0]).getInt(3 * index + 1);
            resultValue.r = ((IntArray)storage[0]).getInt(3 * index + 2);
        }

        public Circle get(long index, Array[] storage) {
            Circle circ = new Circle();
            circ.x = ((IntArray)storage[0]).getInt(3 * index);
            circ.y = ((IntArray)storage[0]).getInt(3 * index + 1);
            circ.r = ((IntArray)storage[0]).getInt(3 * index + 2);
            return circ;
        }

        public void set(long index, Circle  value, UpdatableArray[] storage) {
            Circle circ = value == null ? new Circle() : value;
            ((UpdatableIntArray)storage[0]).setInt(3 * index, circ.x);
            ((UpdatableIntArray)storage[0]).setInt(3 * index + 1, circ.y);
            ((UpdatableIntArray)storage[0]).setInt(3 * index + 2, circ.r);
        }

        public UpdatableArray[] allocateStorage(long length, boolean unresizable) {
            return new UpdatableArray[] {
                unresizable ? mm.newUnresizableIntArray(3 * length) : mm.newIntArray(3 * length)};
        }

        public int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage) {
            return 3;
        }
    }

    static class MyCombinerPacked extends MyCombinerSingle {
        public MyCombinerPacked(MemoryModel mm) {
            super(mm);
        }

        public UpdatableArray[] allocateStorage(long length, boolean unresizable) {
            if (!unresizable)
                throw new UnsupportedOperationException("Packed combiner may be used with unresizable arrays only");
            return new UpdatableArray[]{
                Arrays.asUpdatableFuncArray(true, LinearFunc.getUpdatableInstance(-1000, 1),
                    UpdatableIntArray.class, mm.newUnresizableShortArray(3 * length))};
        }

        public int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage) {
            return 3;
        }
    }

    static class MyCombinerBuffer extends CombinedMemoryModel.AbstractByteBufferCombinerInPlace<Circle> {
        private final IntBuffer ib;

        public MyCombinerBuffer(boolean direct) {
            this(Arrays.SystemSettings.globalMemoryModel(), direct);
        }

        public MyCombinerBuffer(MemoryModel mm, boolean direct) {
            super(Circle.class,
                direct ? ByteBuffer.allocateDirect(12) : ByteBuffer.allocate(12),
                mm);
            this.ib = workStorage.asIntBuffer();
        }

        public Circle allocateElement() {
            return new Circle();
        }

        protected Circle loadElement() {
            Circle circ = new Circle();
            circ.x = ib.get(0);
            circ.y = ib.get(1);
            circ.r = ib.get(2);
            return circ;
        }

        protected void storeElement(Circle element) {
            Circle circ = element == null ? new Circle() : element;
            ib.put(0, circ.x);
            ib.put(1, circ.y);
            ib.put(2, circ.r);
        }

        protected void loadElementInPlace(Circle resultElement) {
            resultElement.x = ib.get(0);
            resultElement.y = ib.get(1);
            resultElement.r = ib.get(2);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + CombinedArraysDemo.class.getName()
                + " ArrayList|CombinedMulti[AsList]|CombinedSingle[AsList]|CombinedPacked|CombinedByteBuffer[AsList]|JavaArray"
                + " arrayLength [-useArrayGet] [-direct]");
            return;
        }
        DemoUtils.initializeClass();
        System.out.println(Arrays.SystemSettings.globalMemoryModel());
        System.out.println();

        boolean useGet = args.length > 2 && "-useArrayGet".equalsIgnoreCase(args[2]);
        boolean direct = args.length > 2 && "-direct".equalsIgnoreCase(args[args.length - 1]);
        int n = Integer.parseInt(args[1]);
        int mode;
        if (args[0].equalsIgnoreCase("ArrayList")) {
            System.out.println("Points are stored in ArrayList");
            mode = 0;
        } else if (args[0].equalsIgnoreCase("CombinedMulti")) {
            System.out.println("Points are stored in a combined array based on 3 \"parallel\" IntArray");
            mode = 1;
        } else if (args[0].equalsIgnoreCase("CombinedSingle")) {
            System.out.println("Points are stored in a combined array based on 1 IntArray");
            mode = 2;
        } else if (args[0].equalsIgnoreCase("CombinedPacked")) {
            System.out.println("Points are stored in a combined array based on 1 IntArray");
            mode = 3;
        } else if (args[0].equalsIgnoreCase("CombinedByteBuffer")) {
            System.out.println("Points are stored in a combined array based on 1 ByteArray "
                + "(via " + (direct ? "direct" : "heap") + " ByteBuffer)");
            mode = 4;
        } else if (args[0].equalsIgnoreCase("CombinedMultiAsList")) {
            System.out.println("Points are stored in a list view of combined array based on 3 "
                + "\"parallel\" IntArray");
            mode = 5;
        } else if (args[0].equalsIgnoreCase("CombinedSingleAsList")) {
            System.out.println("Points are stored in a list view of combined array based on 1 IntArray");
            mode = 6;
        } else if (args[0].equalsIgnoreCase("CombinedByteBufferAsList")) {
            System.out.println("Points are stored in a list view of combined array based on 1 ByteArray "
                + "(via " + (direct ? "direct" : "heap") + " ByteBuffer)");
            mode = 7;
        } else if (args[0].equalsIgnoreCase("JavaArray")) {
            System.out.println("Points are stored in Java int[] array (difficult Java-code)");
            mode = 8;
        } else {
            System.err.println("Unknown storing scheme.");
            return;
        }

        for (int count = 1; count <= 10; count++) {
            System.out.println("Iteration #" + count);
            List<Circle> list = null;
            UpdatableObjectInPlaceArray<Circle> a = null;
            int[] ja = null;

            long t1 = System.nanoTime();
            // Creation
            switch(mode) {
                case 0:
                    list = new ArrayList<Circle>(n);
                    for (int k = 0; k < n; k++)
                        list.add(new Circle());
                    break;
                case 1:
                case 5:
                    a = (UpdatableObjectInPlaceArray<Circle>)CombinedMemoryModel.<Circle>getInstance(
                        new MyCombinerMulti(Arrays.SystemSettings.globalMemoryModel()))
                        .newUnresizableObjectArray(Circle.class, n);
                    break;
                case 2:
                case 6:
                    a = (UpdatableObjectInPlaceArray<Circle>)CombinedMemoryModel.<Circle>getInstance(
                        new MyCombinerSingle(Arrays.SystemSettings.globalMemoryModel()))
                        .newUnresizableObjectArray(Circle.class, n);
                    break;
                case 3:
                    a = (UpdatableObjectInPlaceArray<Circle>)CombinedMemoryModel.<Circle>getInstance(
                        new MyCombinerPacked(Arrays.SystemSettings.globalMemoryModel()))
                        .newUnresizableObjectArray(Circle.class, n);
                    break;
                case 4:
                case 7:
                    a = (UpdatableObjectInPlaceArray<Circle>)CombinedMemoryModel.<Circle>getInstance(
                        new MyCombinerBuffer(direct)).newUnresizableObjectArray(Circle.class, n);
                    break;
                case 8:
                    ja = new int[3 * n];
                    break;
            }
            if (mode == 4 || mode == 5 || mode == 6) {
                list = Arrays.asList(a);
            }

            long t2 = System.nanoTime();
            // Filling
            if (list != null) {
                for (int k = 0; k < n; k++) {
                    Circle circ = list.get(k);
                    circ.x = 2 * k;
                    circ.y = 2 * k;
                    circ.r = n - k;
                    if (a != null)
                        list.set(k, circ);
                }
            } else if (a != null) {
                Circle circ = new Circle();
                for (int k = 0; k < n; k++) {
                    circ.x = 2 * k;
                    circ.y = 2 * k;
                    circ.r = n - k;
                    a.setElement(k, circ);
                }
            } else {
                for (int k = 0, disp = 0; k < n; k++) {
                    ja[disp++] = 2 * k;
                    ja[disp++] = 2 * k;
                    ja[disp++] = n - k;
                }
            }
            Runtime rt = Runtime.getRuntime();
            long currentMemory = rt.totalMemory() - rt.freeMemory();

            long t3 = System.nanoTime();
            // Getting for some calculations
            int sum = 0;
            if (list != null) {
                for (int k = 0; k < n; k++) {
                    Circle circle = list.get(k);
                    sum += circle.x + circle.y + circle.r;
                }
            } else if (a != null) {
                if (useGet) {
                    for (int k = 0; k < n; k++) {
                        Circle circle = a.get(k);
                        sum += circle.x + circle.y + circle.r;
                    }
                } else { // optimal method
                    Circle circ = new Circle();
                    for (int k = 0; k < n; k++) {
                        a.getInPlace(k, circ);
                        sum += circ.x + circ.y + circ.r;
                    }
                }
            } else {
                for (int k = 0, disp = 0; k < n; k++) {
                    sum += ja[disp++] + ja[disp++] + ja[disp++];
                }
            }

            long t4 = System.nanoTime();
            // Sorting array
            if (list != null) {
                Collections.sort(list);
            } else if (a != null) {
                Arrays.sort(a, Arrays.normalOrderComparator(a));
            } else {
                final int[] arr = ja;
                ArraySorter.getQuickSorter().sort(0, n,
                    new ArrayComparator() {
                        public boolean less(long i, long j) {
                            return arr[3 * (int) i] < arr[3 * (int) j];
                        }
                    },
                    new ArrayExchanger() {
                        public void swap(long i, long j) {
                            int temp = arr[3 * (int) i];
                            arr[3 * (int) i] = arr[3 * (int) j];
                            arr[3 * (int) j] = temp;
                            temp = arr[3 * (int) i + 1];
                            arr[3 * (int) i + 1] = arr[3 * (int) j + 1];
                            arr[3 * (int) j + 1] = temp;
                            temp = arr[3 * (int) i + 2];
                            arr[3 * (int) i + 2] = arr[3 * (int) j + 2];
                            arr[3 * (int) j + 2] = temp;
                        }
                    });
            }

            long t5 = System.nanoTime();
            // Making string representation of 1/10 of all points
            String s;
            if (list != null) {
                s = list.subList(0, n / 10).toString();
            } else if (a != null) {
                s = Arrays.toString(a.subArray(0, n / 10), "; ", Integer.MAX_VALUE);
            } else {
                StringBuffer sb = new StringBuffer();
                for (int k = 0, disp = 0, max = n / 10; k < max; k++) {
                    if (k > 0)
                        sb.append("; ");
                    sb.append(ja[disp++]).append(", ");
                    sb.append(ja[disp++]).append(", r=");
                    sb.append(ja[disp++]);
                }
                s = sb.toString();
            }

            long t6 = System.nanoTime();
            if (s.length() > 100)
                s = s.substring(0, 100) + "...";
            if (a != null) {
                System.out.println("Created combined array: " + a);
                String[] stor = CombinedMemoryModel.getStorageToStrings(a);
                System.out.printf(Locale.US, "Storage: %n    "
                    + Arrays.toString(SimpleMemoryModel.asUpdatableArray(stor), "%n    ", 10000)
                    + "%n");
            }
            list = null;
            a = null;
            ja = null;
            System.gc();
            long t7 = System.nanoTime();

            System.out.printf(Locale.US, "Creating circles:             %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / n);
            System.out.printf(Locale.US, "Filling by some circles:      %.3f ms, %.3f ns/element%n",
                (t3 - t2) * 1e-6, (t3 - t2) * 1.0 / n);
            System.out.printf(Locale.US, "    (%.6f MB used, %.3f bytes/element)%n",
                currentMemory / 1048576.0, currentMemory * 1.0 / n);
            System.out.printf(Locale.US, "Loading all data:             %.3f ms, %.3f ns/element%n",
                (t4 - t3) * 1e-6, (t4 - t3) * 1.0 / n);
            System.out.printf(Locale.US, "    (Calculated sum: " + sum
                + (useGet ? "; get method was used" : "") + ")%n");
            System.out.printf(Locale.US, "Sorting by decreasing r:      %.3f ms, %.3f ns/element%n",
                (t5 - t4) * 1e-6, (t5 - t4) * 1.0 / n);
            System.out.printf(Locale.US, "Making string (1/10 of all):  %.3f ms, %.3f ns/element%n",
                (t6 - t5) * 1e-6, (t6 - t6) * 1.0 / n);
            System.out.println("    " + s);
            System.out.printf(Locale.US, "System.gc():                  %.3f ms%n%n", (t7 - t6) * 1e-6);
        }
        DemoUtils.fullGC();
    }
}
