/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Test for {@link Matrices#fillRegion(Matrix, Matrices.Region, Object)} method
 * for a case of {@link Matrices.Polygon2D}.</p>
 *
 * @author Daniel Alievsky
 */
public class FillPolygonTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: " + FillPolygonTest.class.getName()
                    + " numberOfTests elementType style matrixWidth matrixHeight numberOfVertices"
                    + " resultFileName resultAWTFileName [randSeed]");
            return;
        }
        final int numberOfTests = Integer.parseInt(args[0]);
        final String elementTtypeName = args[1];
        final Class<?> elementType  = switch (elementTtypeName) {
            case "bit" -> boolean.class;
            case "char" -> char.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            default -> throw new IllegalArgumentException("Invalid element type");
        };
        final String style = args[2].toLowerCase();
        final long width = Long.parseLong(args[3]);
        final long height = Long.parseLong(args[4]);
        final int numberOfVertices = Integer.parseInt(args[5]);
        final Path resultFile = Path.of(args[6]);
        final Path resultAwtFile = Path.of(args[7]);
        final Random rnd = args.length > 8 ? new Random(Long.parseLong(args[8])) : new Random();

        final double[][] vertices = new double[numberOfVertices][2];
        for (int testCount = 0; testCount < numberOfTests; testCount++) {
            System.out.printf("Test #%d/%d%n", testCount + 1, numberOfTests);
            final boolean roundAll = rnd.nextBoolean();
            final boolean disableRoundingAll = rnd.nextBoolean();
            double brushAngle = 0.25 * Math.PI;
            // - for randombrush style
            for (int k = 0; k < numberOfVertices; k++) {
                boolean enableRounding = !disableRoundingAll;
                if (style.equals("regular")) {
                    final double fi = 2.0 * Math.PI * (double) k / (double) numberOfVertices;
                    vertices[k][0] = 0.5 * width + Math.cos(fi) * 0.5 * (width - 1);
                    vertices[k][1] = 0.5 * height + Math.sin(fi) * 0.5 * (height - 1);
                } else if (style.equals("bigregular")) {
                    final double fi = 2.0 * Math.PI * (double) k / (double) numberOfVertices;
                    vertices[k][0] = 0.5 * width + 1.2 * Math.cos(fi) * 0.5 * (width - 1);
                    vertices[k][1] = 0.5 * height + 1.2 * Math.sin(fi) * 0.5 * (height - 1);
                } else if (style.equals("random")) {
                    vertices[k][0] = rnd.nextDouble() * (width - 1);
                    vertices[k][1] = rnd.nextDouble() * (height - 1);
                } else if (style.equals("bigrandom")) {
                    vertices[k][0] = 3 * (rnd.nextDouble() - 0.5) * (width - 1);
                    vertices[k][1] = 3 * (rnd.nextDouble() - 0.5) * (height - 1);
                } else if (style.equals("randombrush")) {
                    double x = k == 0 ? 0.1 * width : vertices[k - 1][0] + rnd.nextDouble() * 5 * Math.cos(brushAngle);
                    double y = k == 0 ? 0.1 * height : vertices[k - 1][1] + rnd.nextDouble() * 5 * Math.sin(brushAngle);
                    if (y < 0.0) {
                        brushAngle = 0.5 * Math.PI;
                    } else if (y > height) {
                        brushAngle = -0.5 * Math.PI;
                    } else if (x < 0.0) {
                        brushAngle = 0.0;
                    } else if (x > width) {
                        brushAngle = Math.PI;
                    } else {
                        brushAngle += (rnd.nextDouble() - 0.5) * 0.05 * Math.PI;
                    }
                    vertices[k][0] = x;
                    vertices[k][1] = y;
                } else if (style.equals("rectangle")) {
                    int n1 = numberOfVertices / 4, n2 = numberOfVertices / 2, n3 = 3 * numberOfVertices / 4;
                    vertices[k][0] = k < n1 ? (double) k / n1 * width
                            : k < n2 ? 0.9 * width
                            : k < n3 ? (double) (n3 - k) / n1 * width
                            : 0.1 * width;
                    vertices[k][1] = k < n1 ? 0.1 * height
                            : k < n2 ? (double) (k - n1) / n1 * height
                            : k < n3 ? 0.9 * height
                            : (double) (numberOfVertices - k) / n1 * height;
                } else if (style.equals("spiral") || style.equals("bigspiral") || style.equals("largespiral")) {
                    final double fi = 50.0 * Math.PI * (double) k / (double) numberOfVertices;
                    final double r = (style.equals("spiral") ? 0.5 : style.equals("bigspiral") ? 3.0 : 1e9)
                            * (height - 1) * (double) k / (double) numberOfVertices;
                    vertices[k][0] = 0.5 * width + r * Math.cos(fi);
                    vertices[k][1] = 0.5 * height + r * Math.sin(fi);
                } else if (style.equals("horizontal")) {
                    vertices[k][0] = (0.1 + 0.8 * (double) k / (double) (numberOfVertices - 1)) * width;
                    vertices[k][1] = Math.round(0.5 * height);
                } else if (style.equals("largehorizontal")) {
                    vertices[k][0] = 1e9 * (-0.5 + (double) k / (double) (numberOfVertices - 1)) * width;
                    vertices[k][1] = Math.round(0.5 * height);
                } else if (style.equals("emptyhorizontal")) {
                    vertices[k][0] = (0.1 + 0.8 * (double) k / (double) (numberOfVertices - 1)) * width;
                    vertices[k][1] = Math.round(0.5 * height) + 0.1;
                    enableRounding = false;
                } else if (style.equals("vertical")) {
                    vertices[k][0] = Math.round(0.5 * width);
                    vertices[k][1] = (0.1 + 0.8 * (double) k / (double) (numberOfVertices - 1)) * height;
                } else if (style.equals("largevertical")) {
                    vertices[k][0] = Math.round(0.5 * width);
                    vertices[k][1] = 1e9 * (-0.5 + (double) k / (double) (numberOfVertices - 1)) * height;
                } else if (style.equals("emptyvertical")) {
                    vertices[k][0] = Math.round(0.5 * width) + 0.1;
                    vertices[k][1] = (0.1 + 0.8 * (double) k / (double) (numberOfVertices - 1)) * height;
                    enableRounding = false;
                } else {
                    throw new IllegalArgumentException("Unknown polygon style");
                }
                if (enableRounding && (roundAll || rnd.nextBoolean())) {
                    vertices[k][0] = Math.floor(vertices[k][0]);
                }
                if (enableRounding && (roundAll || rnd.nextBoolean())) {
                    vertices[k][1] = Math.floor(vertices[k][1]);
                }
                if (testCount == 0) {
                    if (k < 20) {
                        System.out.printf(Locale.US,
                                "Making vertex #%d: (%.3f, %.3f)%n", k, vertices[k][0], vertices[k][1]);
                    } else if (k == 20) {
                        System.out.println("...");
                    }
                }
            }
            final Matrix<? extends UpdatablePArray> matrix = Arrays.SMM.newMatrix(
                    UpdatablePArray.class, elementType, width, height);
            final Matrices.Polygon2D polygon = Matrices.Region.getPolygon2D(vertices);
            System.out.printf("Filling polygon \"%s\" with %d vertices at matrix %s[%dx%d]...%n",
                    style, numberOfVertices, elementType, width, height);
            long t1 = System.nanoTime();
            Matrices.fillRegion(matrix, polygon, matrix.array().maxPossibleValue(1.0));
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "Polygon filled in %.3f ms%n", (t2 - t1) * 1e-6);
            MatrixIO.writeImage(resultFile, Collections.singletonList(matrix));
            System.out.printf(Locale.US, "Polygon saved in %s%n", resultFile);

            if (width == (int) width && height == (int) height) {
                System.out.printf("Filling polygon by AWT with %d vertices...%n", numberOfVertices);
                final BufferedImage bufferedImage = new BufferedImage(
                        (int) width, (int) height, BufferedImage.TYPE_BYTE_BINARY);
                final Graphics graphics = bufferedImage.getGraphics();
                final int[] xPoints = new int[numberOfVertices];
                final int[] yPoints = new int[numberOfVertices];
                for (int k = 0; k < numberOfVertices; k++) {
                    xPoints[k] = (int) vertices[k][0];
                    yPoints[k] = (int) vertices[k][1];
                }
                t1 = System.nanoTime();
                graphics.fillPolygon(xPoints, yPoints, numberOfVertices);
                t2 = System.nanoTime();
                System.out.printf(Locale.US, "Polygon filled by AWT in %.3f ms%n", (t2 - t1) * 1e-6);
                MatrixIO.writeBufferedImage(resultAwtFile, bufferedImage);
                System.out.printf(Locale.US, "Polygon saved in %s%n", resultAwtFile);
            }
            System.out.println();
        }
        System.out.println("Done");
    }
}
