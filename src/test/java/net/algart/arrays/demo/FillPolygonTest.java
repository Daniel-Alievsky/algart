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

import net.algart.arrays.*;
import net.algart.external.ExternalAlgorithmCaller;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

/**
 * <p>Test for {@link Matrices#fillRegion(ArrayContext, Matrix, Matrices.Region, Object)} method
 * for a case of {@link Matrices.Polygon2D}.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class FillPolygonTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: " + FillPolygonTest.class.getName()
                + " matrixWidth matrixHeight numberOfVertices resultFileName resultAWTFileName [randSeed]");
            return;
        }
        final long width = Long.parseLong(args[0]);
        final long height = Long.parseLong(args[1]);
        final int numberOfVertices = Integer.parseInt(args[2]);
        final File resultFile = new File(args[3]);
        final File resultAwtFile = new File(args[4]);
        final Random rnd = args.length > 5 ? new Random(Long.parseLong(args[5])) : new Random();

        final Matrix<UpdatableBitArray> matrix = Arrays.SMM.newBitMatrix(width, height);
        final double[][] vertices = new double[numberOfVertices][2];
        for (int k = 0; k < numberOfVertices; k++) {
            vertices[k][0] = rnd.nextDouble() * (width - 1);
            vertices[k][1] = rnd.nextDouble() * (height - 1);
        }
        final Matrices.Polygon2D polygon = Matrices.Region.getPolygon2D(vertices);
        System.out.printf("Filling polygon with %d vertices...%n", numberOfVertices);
        long t1 = System.nanoTime();
        Matrices.fillRegion(null, matrix, polygon, 1.0);
        long t2 = System.nanoTime();
        System.out.printf(Locale.US, "Polygon filled in %.3f ms%n", (t2 - t1) * 1e-6);
        ExternalAlgorithmCaller.writeImage(resultFile, Collections.singletonList(matrix));

        if (width == (int) width && height == (int) height) {
            final BufferedImage bufferedImage = new BufferedImage(
                (int) width, (int) height, BufferedImage.TYPE_BYTE_BINARY);
            final Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
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
            ImageIO.write(bufferedImage, ExternalAlgorithmCaller.getFileExtension(resultAwtFile), resultAwtFile);
        }
        System.out.println("Done");
    }
}
