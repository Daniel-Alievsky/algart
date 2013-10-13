package net.algart.arrays.demo;

import net.algart.arrays.ArrayComparator;
import net.algart.arrays.ArrayExchanger;
import net.algart.arrays.ArraySorter;

/**
 * <p>Sorting points: an example from comments to {@link net.algart.arrays.ArraySorter} class</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.1
 */
public class SortingPointsDemo {
    public static void main(String[] args) {
        int n = 10;
        final float[] xy = new float[2 * n];
        for (int k = 0; k < n; k++) {
            xy[2 * k] = (float)Math.random();     // x-coordinate of the point #k
            xy[2 * k + 1] = (float)Math.random(); // y-coordinate of the point #k
        }
        System.out.println(n + " points before sorting:");
        for (int k = 0; k < n; k++)
            System.out.println("x = " + xy[2 * k] + ", y = " + xy[2 * k + 1]);

        ArraySorter.getQuickSorter().sort(0, n,
            new ArrayComparator() {
                public boolean less(long i, long j) {
                    return xy[2 * (int) i] < xy[2 * (int) j];
                }
            },
            new ArrayExchanger() {
                public void swap(long i, long j) {
                    float temp = xy[2 * (int) i];
                    xy[2 * (int) i] = xy[2 * (int) j];
                    xy[2 * (int) j] = temp;
                    temp = xy[2 * (int) i + 1];
                    xy[2 * (int) i + 1] = xy[2 * (int) j + 1];
                    xy[2 * (int) j + 1] = temp;
                }
            });

        System.out.println(n + " points after sorting:");
        for (int k = 0; k < n; k++) {
            System.out.println("x = " + xy[2 * k] + ", y = " + xy[2 * k + 1]);
        }
    }
}
