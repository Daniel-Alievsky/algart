package net.algart.arrays.demo;

import net.algart.arrays.*;
import net.algart.math.functions.ConstantFunc;

import java.util.Locale;

/**
 * <p>Shows the minimal and maximal possible array elements for all element types.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class MinimalAndMaximalPossibleElementsDemo {
    public static void main(String[] args) {
        Class<?>[] types = {boolean.class, char.class, byte.class, short.class,
            int.class, long.class, float.class, double.class,
            String.class
        };
        for (Class<?> t : types) {
            Class<? extends Array> type = Arrays.type(Array.class, t);
            PArray[] arrays = new PArray[5];
            double min = Arrays.minPossibleValue(type, t.isPrimitive() ? Double.NEGATIVE_INFINITY : Double.NaN);
            double max = Arrays.maxPossibleValue(type, t.isPrimitive() ? Double.POSITIVE_INFINITY : Double.NaN);
            System.out.printf(Locale.US,
                "%-16s - minimal possible element is %.1f, maximal possible element is %.1f%n",
                t.getCanonicalName(), min, max);
            if (t.isPrimitive()) {
                arrays[0] = (PArray)SimpleMemoryModel.getInstance().newEmptyArray(t);
                arrays[1] = (PArray)BufferMemoryModel.getInstance().newEmptyArray(t);
                arrays[2] = (PArray)LargeMemoryModel.getInstance().newEmptyArray(t);
                arrays[3] = Arrays.nPCopies(1, t, 1);
                assert arrays[3] != null;
                arrays[4] = Arrays.asIndexFuncArray(ConstantFunc.getInstance(0), Arrays.type(PArray.class, t), 1);
                for (PArray a : arrays) {
                    if (a.minPossibleValue(Double.NEGATIVE_INFINITY) != min)
                        throw new AssertionError("Illegal minValue(double) in " + a);
                    if (a.maxPossibleValue(Double.POSITIVE_INFINITY) != max)
                        throw new AssertionError("Illegal maxValue(double) in " + a);
                    if (a instanceof PFixedArray) {
                        if (((PFixedArray)a).minPossibleValue() != min)
                            throw new AssertionError("Illegal minValue() in " + a);
                        if (((PFixedArray)a).maxPossibleValue() != max)
                            throw new AssertionError("Illegal maxValue() in " + a);
                    }
                }
            }
        }
    }
}
