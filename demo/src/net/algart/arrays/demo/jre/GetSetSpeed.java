/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays.demo.jre;

/**
 * <p>Speed of get/set methods in comparison with direct field access</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class GetSetSpeed {
    static long nanoTime() {
//        return net.algart.performance.Time.currentTimeNanoseconds();
        return System.nanoTime();

    }

    static String toString(double value, int d) {
        return String.format("%.8f", value);
    }

    public static void main(String[] args) {
        Test as = new Test();
        final int n = 100000000;
        final int n10 = 10 * n;
        int v1 = 0, v2 = 0, v3 = 0, v4 = 0, v5 = 0, v6 = 0, v7 = 0, v8 = 0, v9 = 0, v10 = 0;
        for (int k = 0; k < n; k++) { // fill all possible caches
            as.setA(as.getA());
        }
        long t1 = nanoTime();
        for (int k = 0; k < n10; k++) {
            v1 = as.a;
        }
        long t11 = nanoTime();
        for (int k = 0; k < n; k++) {
            v1 = as.a;
            v2 = as.a;
            v3 = as.a;
            v4 = as.a;
            v5 = as.a;
            v6 = as.a;
            v7 = as.a;
            v8 = as.a;
            v9 = as.a;
            v10 = as.a;
        }
        long t2 = nanoTime();
        for (int k = 0; k < n10; k++) {
            v1 = as.getA();
        }
        long t21 = nanoTime();
        for (int k = 0; k < n; k++) {
            v1 = as.getA();
            v2 = as.getA();
            v3 = as.getA();
            v4 = as.getA();
            v5 = as.getA();
            v6 = as.getA();
            v7 = as.getA();
            v8 = as.getA();
            v9 = as.getA();
            v10 = as.getA();
        }
        long t3 = nanoTime();
        for (int k = 0; k < n10; k++) {
            as.setA(v1);
        }
        long t31 = nanoTime();
        for (int k = 0; k < n; k++) {
            as.setA(v1);
            as.setA(v2);
            as.setA(v3);
            as.setA(v4);
            as.setA(v5);
            as.setA(v6);
            as.setA(v7);
            as.setA(v8);
            as.setA(v9);
            as.setA(v10);
        }
        long t4 = nanoTime();
        for (int k = 0; k < n10; k++) {
            as.a += as.a;
        }
        long t41 = nanoTime();
        for (int k = 0; k < n; k++) {
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
            as.a += as.a;
        }
        long t5 = nanoTime();
        for (int k = 0; k < n10; k++) {
            as.setA(as.getA() + as.getA());
        }
        long t51 = nanoTime();
        for (int k = 0; k < n; k++) {
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
            as.setA(as.getA() + as.getA());
        }
        long t6 = nanoTime();
        System.out.println(toString((t11 - t1 + 0.0) / n10, 8) + " | "
            + toString((t2 - t11 + 0.0) / n10, 8)  + " ns field");
        System.out.println(toString((t21 - t2 + 0.0) / n10, 8) + " | "
            + toString((t3 - t21 + 0.0) / n10, 8)  + " ns get");
        System.out.println(toString((t31 - t3 + 0.0) / n10, 8) + " | "
            + toString((t4 - t31 + 0.0) / n10, 8)  + " ns set");
        System.out.println(toString((t41 - t4 + 0.0) / n10, 8) + " | "
            + toString((t5 - t41 + 0.0) / n10, 8)  + " ns a+=a");
        System.out.println(toString((t51 - t5 + 0.0) / n10, 8) + " | "
            + toString((t6 - t51 + 0.0) / n10, 8)  + " ns set(get()+get())");
    }

    static class Test {
        public int a = 1;

        public int getA() {
            return a;
        }

        public void setA(int value) {
            a = value;
        }
    }
}
