/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.lang.reflect.Field;
import java.util.Locale;

import net.algart.arrays.*;

/**
 * <p>The test illustrating that all AlgART arrays, including file-based, are initialized by zeros.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class ZeroInitializingTest {
    private static long mySum(Array array, boolean reverseOrder) {
        long result = 0;
        if (!reverseOrder) {
            if (array instanceof PArray) {
                result = (long)Arrays.sumOf((PArray)array);
            } else if (array.elementType() == CombinedArraysDemo.Circle.class) {
                ObjectArray<CombinedArraysDemo.Circle> a = ((ObjectArray<?>)array).cast(CombinedArraysDemo.Circle.class);
                for (long k = 0, n = a.length(); k < n; k++) {
                    result += a.get(k).r;
                }
            } else {
                throw new IllegalArgumentException("Unknown type");
            }
        } else {
            if (array instanceof PArray) {
                for (long q = array.length(); q > 0; ) {
                    long p = Math.max(q - 10000, 0);
                    result += (long)Arrays.sumOf((PArray)array.subArray(p, q));
                    q = p;
                }
            } else if (array.elementType() == CombinedArraysDemo.Circle.class) {
                ObjectArray<CombinedArraysDemo.Circle> a = ((ObjectArray<?>)array).cast(CombinedArraysDemo.Circle.class);
                for (long k = a.length() - 1; k >= 0; k--) {
                    result += a.get(k).r;
                }
            } else {
                throw new IllegalArgumentException("Unknown type");
            }
        }
        return result;
    }

    private static long mySumSimple(Array array, boolean reverseOrder) {
        if (!(array instanceof PArray))
            return mySum(array, reverseOrder);
        long result = 0;
        if (!reverseOrder) {
            if (array instanceof PFixedArray) {
                for (long k = 0, n = array.length(); k < n; k++) {
                    result += ((PFixedArray)array).getLong(k);
                }
            } else {
                double sum = 0.0;
                for (long k = 0, n = array.length(); k < n; k++) {
                    sum += ((PArray)array).getDouble(k);
                }
                result = (long)sum;
            }
        } else {
            for (long q = array.length(); q > 0; ) {
                long p = Math.max(q - 10000, 0);
                result += mySumSimple(array.subArray(p, q), false);
                q = p;
            }
        }
        return result;
    }

    private static long myFillBy1(UpdatableArray array) {
        if (array instanceof UpdatableBitArray) {
            ((UpdatableBitArray)array).fill(true);
        } else if (array instanceof UpdatableCharArray) {
            ((UpdatableCharArray)array).fill((char)1);
        } else if (array instanceof UpdatableByteArray) {
            ((UpdatableByteArray)array).fill((byte)1);
        } else if (array instanceof UpdatableShortArray) {
            ((UpdatableShortArray)array).fill((short)1);
        } else if (array instanceof UpdatableIntArray) {
            ((UpdatableIntArray)array).fill(1);
        } else if (array instanceof UpdatableLongArray) {
            ((UpdatableLongArray)array).fill(1L);
        } else if (array instanceof UpdatableFloatArray) {
            ((UpdatableFloatArray)array).fill(1.0f);
        } else if (array instanceof UpdatableDoubleArray) {
            ((UpdatableDoubleArray)array).fill(1.0);
        } else if (array.elementType() == CombinedArraysDemo.Circle.class) {
            UpdatableObjectArray<?> oa = (UpdatableObjectArray<?>)array;
            UpdatableObjectArray<CombinedArraysDemo.Circle> a = oa.cast(CombinedArraysDemo.Circle.class);
            a.fill(new CombinedArraysDemo.Circle(12, 157, 1));
        } else {
            throw new IllegalArgumentException("Unknown type");
        }
        return array.length();
    }

    private static long myFillByProgression(UpdatableArray array, boolean reverseOrder) {
        long result = 0;
        if (!reverseOrder) {
            if (array instanceof UpdatableBitArray) {
                UpdatableBitArray a = (UpdatableBitArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setBit(k, k % 2 == 1);
                    result += k % 2 == 1 ? 1 : 0;
                }
            } else if (array instanceof UpdatableCharArray) {
                UpdatableCharArray a = (UpdatableCharArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setChar(k, (char)k);
                    result += (char)k;
                }
            } else if (array instanceof UpdatableByteArray) {
                UpdatableByteArray a = (UpdatableByteArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setByte(k, (byte)k);
                    result += ((byte)k) & 0xFF;
                }
            } else if (array instanceof UpdatableShortArray) {
                UpdatableShortArray a = (UpdatableShortArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setShort(k, (short)k);
                    result += ((short)k) & 0xFFFF;
                }
            } else if (array instanceof UpdatableIntArray) {
                UpdatableIntArray a = (UpdatableIntArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setInt(k, (int)k);
                    result += (int)k;
                }
            } else if (array instanceof UpdatableLongArray) {
                UpdatableLongArray a = (UpdatableLongArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setLong(k, k);
                    result += k;
                }
            } else if (array instanceof UpdatableFloatArray) {
                UpdatableFloatArray a = (UpdatableFloatArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setFloat(k, k);
                    result += (long)(float)k;
                }
            } else if (array instanceof UpdatableDoubleArray) {
                UpdatableDoubleArray a = (UpdatableDoubleArray)array;
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.setDouble(k, k);
                    result += (long)(double)k;
                }
            } else if (array.elementType() == CombinedArraysDemo.Circle.class) {
                UpdatableObjectArray<CombinedArraysDemo.Circle> a =
                    ((UpdatableObjectArray<?>)array).cast(CombinedArraysDemo.Circle.class);
                for (long k = 0, n = a.length(); k < n; k++) {
                    a.set(k, new CombinedArraysDemo.Circle(12, 157, (int)k));
                    result += (int)k;
                }
            } else {
                throw new IllegalArgumentException("Unknown type");
            }
        } else {
            if (array instanceof UpdatableBitArray) {
                UpdatableBitArray a = (UpdatableBitArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setBit(k, k % 2 == 1);
                    result += k % 2 == 1 ? 1 : 0;
                }
            } else if (array instanceof UpdatableCharArray) {
                UpdatableCharArray a = (UpdatableCharArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setChar(k, (char)k);
                    result += (char)k;
                }
            } else if (array instanceof UpdatableByteArray) {
                UpdatableByteArray a = (UpdatableByteArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setByte(k, (byte)k);
                    result += (byte)k;
                }
            } else if (array instanceof UpdatableShortArray) {
                UpdatableShortArray a = (UpdatableShortArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setShort(k, (short)k);
                    result += (short)k;
                }
            } else if (array instanceof UpdatableIntArray) {
                UpdatableIntArray a = (UpdatableIntArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setInt(k, (int)k);
                    result += (int)k;
                }
            } else if (array instanceof UpdatableLongArray) {
                UpdatableLongArray a = (UpdatableLongArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setLong(k, k);
                    result += k;
                }
            } else if (array instanceof UpdatableFloatArray) {
                UpdatableFloatArray a = (UpdatableFloatArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setFloat(k, k);
                    result += (long)(float)k;
                }
            } else if (array instanceof UpdatableDoubleArray) {
                UpdatableDoubleArray a = (UpdatableDoubleArray)array;
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.setDouble(k, k);
                    result += (long)(double)k;
                }
            } else if (array.elementType() == CombinedArraysDemo.Circle.class) {
                UpdatableObjectArray<CombinedArraysDemo.Circle> a =
                    ((UpdatableObjectArray<?>)array).cast(CombinedArraysDemo.Circle.class);
                for (long k = a.length() - 1; k >= 0; k--) {
                    a.set(k, new CombinedArraysDemo.Circle(12, 157, (int)k));
                    result += (int)k;
                }
            } else {
                throw new IllegalArgumentException("Unknown type");
            }
        }
        return result;
    }

    public static void main(String[] args) {
        int startArgIndex = 0;
        boolean unresizable = false, noZeroInit = false, reverseOrder = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-unresizable")) {
            unresizable = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-noZeroInit")) {
            noZeroInit = true; startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-reverseOrder")) {
            reverseOrder = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage: " + ZeroInitializingTest.class.getName()
                + " [-unresizable] [-noZeroInit] [-reverseOrder]"
                + " primitiveType|CombinedMulti|CombinedSingle arrayLength "
                + " [numberOfIterations [initFiller]]");
            System.out.println("-noZeroInit and initFiller are applicable for LARGE memory model only;");
            System.out.println("in this case the package-protected constants are changed via reflection.");
            return;
        }
        MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        String elementTypeName = args[startArgIndex];
        long len = Long.parseLong(args[startArgIndex + 1]);
        int numberOfIterations = startArgIndex + 2 < args.length ? Integer.parseInt(args[startArgIndex + 2]) : 1;
        if (startArgIndex + 3 < args.length) {
            byte zeroFiller = Byte.parseByte(args[startArgIndex + 3]);
            try {
                Class<?> c = Class.forName("net.algart.arrays.DataStorage");
                Field f = c.getDeclaredField("ZERO_INIT_FILLER");
                f.setAccessible(true);
                f.setByte(null, zeroFiller);
                System.out.println("Non-standard filler " + zeroFiller + " for initialization will be used");
            } catch (Exception ex) {
                throw new AssertionError("Cannot set net.algart.arrays.DataStorage.ZERO_INIT_FILLER: " + ex);
            }
        }

        if (noZeroInit) {
            try {
                Class<?> c = Class.forName("net.algart.arrays.DataStorage");
                Field f = c.getDeclaredField("DO_LAZY_INIT");
                f.setAccessible(true);
                f.setBoolean(null, false);
                System.out.println("Lazy initialization will not be used");
            } catch (Exception ex) {
                throw new AssertionError("Cannot set net.algart.arrays.DataStorage.ZERO_INIT_FILLER: " + ex);
            }
        }
        Class<?> clazz =
            elementTypeName.equals("boolean") ? boolean.class :
            elementTypeName.equals("char") ? char.class :
            elementTypeName.equals("byte") ? byte.class :
            elementTypeName.equals("short") ? short.class :
            elementTypeName.equals("int") ? int.class :
            elementTypeName.equals("long") ? long.class :
            elementTypeName.equals("float") ? float.class :
            elementTypeName.equals("double") ? double.class :
            elementTypeName.equals("CombinedMulti") ||
            elementTypeName.equals("CombinedSingle") ? CombinedArraysDemo.Circle.class :
            null;
        if (clazz == null)
            throw new IllegalArgumentException("Unknown element type: " + elementTypeName);
        if (elementTypeName.equals("CombinedMulti")) {
            mm = CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerMulti(mm));
        } else if (elementTypeName.equals("CombinedSingle")) {
            mm = CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerSingle(mm));
        }
        System.out.println(mm);
        System.out.println();

        long t1, t2;
        for (int count = 1; count <= numberOfIterations; count++) {
            System.out.println("*** ITERATION #" + count + " / " + numberOfIterations);
            t1 = System.nanoTime();
            UpdatableArray a;
            if (unresizable) {
                a = mm.newUnresizableArray(clazz, len);
            } else {
                a = mm.newEmptyArray(clazz);
                ((MutableArray)a).length(len / 2);
                ((MutableArray)a).length(len);
            }
            t2 = System.nanoTime();
            System.out.println("Array created: " + a);
            System.out.println("Number of parallel tasks: "
                + DefaultThreadPoolFactory.getDefaultThreadPoolFactory().recommendedNumberOfTasks(a));
            System.out.printf(Locale.US, "Creating array time:         %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            long sum1 = mySum(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time:        %.3f ms, %.3f ns/element (sum = "
                + sum1 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            a.flushResources(null);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Flushing:                    %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            sum1 = mySum(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time:        %.3f ms, %.3f ns/element (sum = "
                + sum1 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            sum1 = myFillByProgression(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Filling by progression time: %.3f ms, %.3f ns/element (sum = "
                + sum1 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            long sum2 = mySum(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time:        %.3f ms, %.3f ns/element (sum = "
                + sum2 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            long sum3 = mySumSimple(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time (loop): %.3f ms, %.3f ns/element (sum = "
                + sum3 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            if (a instanceof PFixedArray && (sum1 != sum2 || sum1 != sum3))
                throw new AssertionError("Different sums!");

            t1 = System.nanoTime();
            Arrays.zeroFill(a);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Filling by 0 time:           %.3f ms, %.3f ns/element%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            sum1 = mySum(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time:        %.3f ms, %.3f ns/element (sum = "
                + sum1 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            sum1 = myFillBy1(a);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Filling by 1 time:           %.3f ms, %.3f ns/element (sum = "
                + sum1 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);

            t1 = System.nanoTime();
            sum2 = mySum(a, reverseOrder);
            t2 = System.nanoTime();
            System.out.printf(Locale.US, "Calculating sum time:        %.3f ms, %.3f ns/element (sum = "
                + sum2 + ")%n",
                (t2 - t1) * 1e-6, (t2 - t1) * 1.0 / len);
            if (a instanceof PFixedArray && sum1 != sum2)
                throw new AssertionError("Different sums!");
            System.out.println();
        }
        t1 = System.nanoTime();
        try {
            Arrays.gcAndAwaitFinalization(15000);
        } catch (InterruptedException ex) {
            // nothing to do here
        }
        t2 = System.nanoTime();
        System.out.printf(Locale.US, "All Java garbage is collected: %.3f ms", (t2 - t1) * 1e-6);

    }
}
