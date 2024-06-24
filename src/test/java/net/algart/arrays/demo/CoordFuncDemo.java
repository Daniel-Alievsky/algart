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

import net.algart.arrays.*;
import net.algart.io.MatrixIO;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.Func2;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CoordFuncDemo {
    private static double myFunc(double x, double y) {
        return (x + y) * 0.0001;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.printf("Usage: %s target_image.jpg/png/bmp " +
                            "dimX dimY%n",
                    CoordFuncDemo.class);
            return;
        }
        final Path targetFile = Paths.get(args[0]);
        final int dimX = Integer.parseInt(args[1]);
        final int dimY = Integer.parseInt(args[2]);

        Func fSlow = new AbstractFunc() {
            @Override
            public double get(double... x) {
                return myFunc(x[0], x[1]);
            }
        };
        Func fQuick = new AbstractFunc() {
            @Override
            public double get(double... x) {
                return myFunc(x[0], x[1]);
            }

            @Override
            public double get(double x0, double x1) {
                return myFunc(x0, x1);
            }
        };
        Func2 fLambda = CoordFuncDemo::myFunc;
        Matrix<UpdatablePArray> m1 = Matrix.newMatrix(float.class, dimX, dimY);
        Matrix<UpdatablePArray> m2 = Matrix.newMatrix(float.class, dimX, dimY);
        Matrix<UpdatablePArray> m3 = Matrix.newMatrix(float.class, dimX, dimY);
        ArrayContext context = null;
        for (int test = 1; test <= 16; test++) {
            System.out.printf("%nTest #%d...%n", test);
            long t1 = System.nanoTime();
            Matrices.copy(context, m1, Matrices.asCoordFuncMatrix(fSlow, FloatArray.class, dimX, dimY));
            long t2 = System.nanoTime();
            Matrices.copy(context, m2, Matrices.asCoordFuncMatrix(fQuick, FloatArray.class, dimX, dimY));
            long t3 = System.nanoTime();
            Matrices.copy(context, m3, Matrices.asCoordFuncMatrix(fLambda, FloatArray.class, dimX, dimY));
            long t4 = System.nanoTime();
            if (!m1.equals(m2)) {
                throw new AssertionError("Bug in fQuick");
            }
            if (!m1.equals(m3)) {
                throw new AssertionError("Bug in fLambda");
            }
            System.out.printf("Slow: %.3f ms%n", (t2 - t1) * 1e-6);
            System.out.printf("Quick: %.3f ms%n", (t3 - t2) * 1e-6);
            System.out.printf("Lambda: %.3f ms%n", (t4 - t3) * 1e-6);
        }
        System.out.printf("%nWriting %s...%n", targetFile);
        MatrixIO.writeImage(targetFile, List.of(m1));
        System.out.println("Done");
    }
}
