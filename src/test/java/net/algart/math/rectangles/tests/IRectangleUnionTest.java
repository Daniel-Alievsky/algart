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

package net.algart.math.rectangles.tests;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.io.MatrixIO;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.math.rectangles.IRectanglesUnion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

public class IRectangleUnionTest {

    private static final boolean ACTUAL_CALL_FIND_METHODS = true;

    public static void main(String[] args) throws IOException {
        boolean awt = false;
        boolean parallel = false;
        int startArgIndex = 0;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-awt")) {
            awt = true;
            startArgIndex++;
        }
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-parallel")) {
            parallel = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 3) {
            System.out.println("Usage:");
            System.out.println("    " + IRectangleUnionTest.class.getName()
                + " [-awt] [-parallel]"
                + " numberOfTests rectangles-description.txt demo-files-folder [coordinate-divider]");
            return;
        }
        final int numberOfTests = Integer.parseInt(args[startArgIndex]);
        final File rectanglesFile = new File(args[startArgIndex + 1]);
        final File demoFolder = new File(args[startArgIndex + 2]);
        final long divider = args.length > startArgIndex + 3 ? Long.parseLong(args[startArgIndex + 3]) : 1;
        //noinspection ResultOfMethodCallIgnored
        demoFolder.mkdirs();
        String[] description = parseConfigurationFile(rectanglesFile);
        final String algorithm = description[0];
        Long width = null;
        Long height = null;
        final List<IRectangularArea> rectangles = new ArrayList<IRectangularArea>();
        if (algorithm.equals("rectangles")) {
            System.out.println("Reading rectangles...");
            for (int k = 1; k < description.length; k++) {
                final String[] rectangle = description[k].split("[\\s,]+");
                rectangles.add(IRectangularArea.of(
                        IPoint.of(Integer.parseInt(rectangle[0]), Integer.parseInt(rectangle[1])),
                        IPoint.of(Integer.parseInt(rectangle[2]), Integer.parseInt(rectangle[3]))));
            }
        } else {
            System.out.println("Creating rectangles...");
            if (algorithm.equals("regular")) {
                final int frameMinWidth = Integer.parseInt(description[1]);
                final int frameMaxWidth = Integer.parseInt(description[2]);
                final int frameMinHeight = Integer.parseInt(description[3]);
                final int frameMaxHeight = Integer.parseInt(description[4]);
                if (frameMinWidth > frameMaxWidth) {
                    throw new IOException("frameMinWidth > frameMaxHeight");
                }
                if (frameMinHeight > frameMaxHeight) {
                    throw new IOException("frameMinHeight > frameMaxHeight");
                }
                width = Long.parseLong(description[5]);
                height = Long.parseLong(description[6]);
                if (width <= 0) {
                    width = null;
                }
                if (height <= 0) {
                    height = null;
                }
                final int horizontalCount = Integer.parseInt(description[7]);
                final int verticalCount = Integer.parseInt(description[8]);
                final int overlap = Integer.parseInt(description[9]);
                final int maxError = Integer.parseInt(description[10]);
                final int randSeed = Integer.parseInt(description[11]);
                final Random rnd = randSeed == -1 ? new Random() : new Random(randSeed);
                final int averageWidth = (frameMinWidth + frameMaxWidth) / 2;
                final int averageHeight = (frameMinHeight + frameMaxHeight) / 2;
                for (int i = 0; i < verticalCount; i++) {
                    for (int j = horizontalCount - 1; j >= 0; j--) {
                        final int frameWidth = frameMinWidth + rnd.nextInt(frameMaxWidth - frameMinWidth + 1);
                        final int frameHeight = frameMinHeight + rnd.nextInt(frameMaxHeight - frameMinHeight + 1);
                        final long x = Math.max(-5,
                            (j + 1) * (averageWidth - overlap) + rnd.nextInt(maxError + 1) - maxError / 2);
                        final long y = Math.max(5,
                            (i + 1) * (averageHeight - overlap) + rnd.nextInt(maxError + 1) - maxError / 2);
                        final IRectangularArea r = IRectangularArea.ofSize(x, y, frameWidth, frameHeight);
                        if (rectangles.size() < 10) {
                            System.out.printf("Frame #%d %dx%d: %s%n",
                                    rectangles.size(), r.size(0), r.size(1), r);
                        } else if (rectangles.size() == 10) {
                            System.out.println("...");
                        }
                        rectangles.add(r);
                    }
                }
            } else {
                throw new IOException("Unknown generating algorithm " + algorithm);
            }
        }
        final IRectanglesUnion rectangleUnion = IRectanglesUnion.newInstance(rectangles);
        final IRectangularArea circumscribedRectangle = rectangleUnion.circumscribedRectangle();
        if (width == null) {
            width = circumscribedRectangle == null ? 100 : circumscribedRectangle.max(0) + 100;
        }
        if (height == null) {
            height = circumscribedRectangle == null ? 100 : circumscribedRectangle.max(1) + 100;
        }

        final List<Matrix<? extends UpdatablePArray>> demo = newImage(width / divider, height / divider);
        draw(demo, circumscribedRectangle, divider, Color.YELLOW, Color.BLUE);
        for (IRectangularArea area : rectangles) {
            draw(demo, area, divider, Color.LIGHT_GRAY, Color.DARK_GRAY);
        }
        final File sourceFile = new File(demoFolder, rectanglesFile.getName() + ".source.bmp");
        System.out.printf("Writing source image %dx%d into %s: %d rectangles%n",
            width, height, sourceFile, rectangles.size());
        MatrixIO.writeImage(sourceFile.toPath(), demo);

        final Thread[] tests = new Thread[numberOfTests];
        for (int ti = 0; ti < numberOfTests; ti++) {
            final int testIndex = ti;
            final long imageWidth = width;
            final long imageHeight = height;
            tests[ti] = new Thread(() -> {
                try {
                    System.out.printf("%nTest #%d%n", testIndex + 1);
                    if (ACTUAL_CALL_FIND_METHODS) {
                        rectangleUnion.findConnectedComponents();
                    }
                    List<Matrix<? extends UpdatablePArray>> demo1 = null;
                    for (int k = -1; k < Math.min(7, rectangleUnion.connectedComponentCount()); k++) {
                        final IRectanglesUnion component = k == -1 ?
                            rectangleUnion :
                            rectangleUnion.connectedComponent(k);
                        System.out.println("Processing " + (k == -1 ? "all union" : "connected component #" + k));
                        if (ACTUAL_CALL_FIND_METHODS) {
                            component.findBoundaries();
                            component.findLargestRectangleInUnion();
                        }
                        if (testIndex == 0) {
                            System.out.println();
                            demo1 = drawUnion(imageWidth, imageHeight, component, divider);
                            File f = new File(demoFolder, rectanglesFile.getName()
                                + (k == -1 ? ".all" : ".component-" + k) + ".bmp");
                            System.out.println("Writing " + (k == -1 ? "all union" : "component #" + k)
                                + " into " + f + ": " + component);
                            MatrixIO.writeImage(f.toPath(), demo1);
                        }
                        IRectanglesUnion currentUnion = component;
                        for (int index = 0; index < 3; index++) {
                            if (testIndex == 0) {
                                if (index == 0) {
                                    demo1 = drawRectangles(imageWidth, imageHeight, currentUnion, divider);
                                } else {
                                    final int v = 255 - index * 50;
                                    draw(demo1, currentUnion.largestRectangleInUnion(), divider,
                                        Color.GREEN, new Color(v, v, v));
                                }
                                File f = new File(demoFolder, rectanglesFile.getName()
                                    + (k == -1 ? ".all" : ".component-" + k) + ".rectangles-" + index + ".bmp");
                                System.out.println("Writing " + (k == -1 ? "all union" : "component #" + k)
                                    + " largest rectangles information into " + f + ": " + currentUnion);
                                MatrixIO.writeImage(f.toPath(), demo1);
                            }
                            long t1 = System.nanoTime();
                            final IRectanglesUnion newUnion = currentUnion.subtractLargestRectangle();
                            long t2 = System.nanoTime();
                            final IRectangularArea largest = currentUnion.largestRectangleInUnion();
                            System.out.printf(Locale.US,
                                "Largest rectangle %s (area %.1f) subtracted from the union %s (area %.1f) "
                                    + "in %.3f ms%n",
                                largest, largest == null ? Double.NaN : largest.volume(),
                                currentUnion, currentUnion.unionArea(), (t2 - t1) * 1e-6);
                            currentUnion = newUnion;
                            if (largest == null) {
                                break;
                            }
                        }
                        if (testIndex == 0) {
                            int index = 0;
                            for (List<IRectanglesUnion.BoundaryLink> boundary : component.allBoundaries()) {
                                demo1 = drawBoundary(imageWidth, imageHeight, boundary, divider, false);
                                File f = new File(demoFolder, rectanglesFile.getName()
                                    + (k == -1 ? ".boundaries" : ".boundary-" + k) + "-" + index + ".bmp");
                                System.out.println("Writing boundary #" + index + " of "
                                    + (k == -1 ? "all union" : "component #" + k)
                                    + " into " + f);
                                MatrixIO.writeImage(f.toPath(), demo1);
                                demo1 = drawBoundary(imageWidth, imageHeight, boundary, divider, true);
                                f = new File(demoFolder, rectanglesFile.getName()
                                    + (k == -1 ? ".boundaries" : ".boundary-" + k) + "-" + index + "-precise.bmp");
                                System.out.println("Writing precise boundary #" + index + " of "
                                    + (k == -1 ? "all union" : "component #" + k)
                                    + " into " + f);
                                MatrixIO.writeImage(f.toPath(), demo1);
                                index++;
                                if (index > 5) {
                                    break;
                                }
                            }
                        }
                        if (k != -1) {
                            // it is a connected component
                            final List<List<IRectanglesUnion.BoundaryLink>> boundaries = component.allBoundaries();
                            final int m = boundaries.size();
                            final double area = IRectanglesUnion.areaInBoundary(boundaries.getFirst());
                            System.out.printf("Area inside boundary #0/%d, connected component #%d: %.1f%n",
                                m, k, area);
                            if (area <= 0) {
                                throw new AssertionError("First area must be positive!");
                            }
                            for (int i = 1; i < m; i++) {
                                final double holeArea = IRectanglesUnion.areaInBoundary(boundaries.get(i));
                                if (i < 5) {
                                    System.out.printf("Area inside boundary #%d/%d (a hole), "
                                            + "connected component %d: %.1f%n", i, m, k, holeArea);
                                } else if (i == 5) {
                                    System.out.println("...");
                                }
                                if (holeArea >= 0) {
                                    throw new AssertionError("Hole area must be negative!");
                                }
                            }
                        }
                        System.out.println();
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                    System.exit(1);
                }
            });
        }
        if (parallel) {
            for (Thread test : tests) {
                test.start();
            }
            for (Thread test : tests) {
                try {
                    test.join();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }
        } else {
            for (Thread test : tests) {
                //noinspection CallToThreadRun
                test.run();
            }
        }
        if (awt) {
            for (int testIndex = 0; testIndex < numberOfTests; testIndex++) {
                System.out.printf("%nAWT test #%d%n", testIndex + 1);
                Area area = new Area();
                long t1 = System.nanoTime();
                for (IRectangularArea r : rectangles) {
                    Shape shape = new Rectangle2D.Double(
                            r.min(0), r.min(1), r.size(0), r.size(1));
                    area.add(new Area(shape));
                }
                long t2 = System.nanoTime();
                int count = 0;
                for (PathIterator pi = area.getPathIterator(null); !pi.isDone(); pi.next()) {
                    count++;
                }
                System.out.printf("AWT area: %.3f ms building (%d elements)%n", (t2 - t1) * 1e-6, count);
                if (testIndex == 0) {
                    BufferedImage bufferedImage = new BufferedImage(
                        (int) (width / divider), (int) (height / divider), BufferedImage.TYPE_INT_BGR);
                    final Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
                    g.setTransform(AffineTransform.getScaleInstance(1.0 / divider, 1.0 / divider));
                    g.setColor(Color.DARK_GRAY);
                    g.fill(area);
                    g.setColor(Color.RED);
                    g.draw(area);
                    int value = 256;
                    count = 0;
                    double[] c1 = null;
                    for (PathIterator pi = area.getPathIterator(null); !pi.isDone(); pi.next()) {
                        if (value >= 256) {
                            value = 63;
                        }
                        double[] c2 = new double[6];
                        final int type = pi.currentSegment(c2);
                        if (type == PathIterator.SEG_MOVETO) {
                            value = 31;
                        } else if (c1 != null && type == PathIterator.SEG_LINETO) {
                            g.setColor(new Color(value, value, 0));
                            g.drawLine((int) c1[0], (int) c1[1], (int) c2[0], (int) c2[1]);
                        }
                        count++;
                        if (count < 10) {
                            System.out.printf(
                                "AWT path iteration #%d: type %d, rule %d, coordinates %.1f, %.1f%n",
                                count, type, pi.getWindingRule(), c2[0], c2[1]);
                        } else if (count == 20) {
                            System.out.println("...");
                        }
                        c1 = c2;
                        value += 32;
                    }
                    File f = new File(demoFolder, rectanglesFile.getName() + ".awt.bmp");
                    System.out.println();
                    System.out.println("Writing AWT path into " + f);
                    ImageIO.write(bufferedImage, "bmp", f);
                }
            }
        }
    }

    private static String[] parseConfigurationFile(File rectanglesFile) throws IOException {
        final List<String> result = new ArrayList<String>();
        for (String s : Files.readString(rectanglesFile.toPath()).split("[\\r\\n]+")) {
            final int p = s.indexOf("//");
            if (p != -1) {
                s = s.substring(0, p);
            }
            s = s.trim();
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result.toArray(new String[0]);
    }

    private static List<Matrix<? extends UpdatablePArray>> newImage(long width, long height) {
        ArrayList<Matrix<? extends UpdatablePArray>> result = new ArrayList<Matrix<? extends UpdatablePArray>>();
        result.add(Arrays.SMM.newByteMatrix(width, height));
        result.add(Arrays.SMM.newByteMatrix(width, height));
        result.add(Arrays.SMM.newByteMatrix(width, height));
        return result;
    }

    private static List<Matrix<? extends UpdatablePArray>> drawUnion(
        long imageWidth, long imageHeight,
        IRectanglesUnion union,
        long divider)
    {
        final List<Matrix<? extends UpdatablePArray>> result = newImage(imageWidth / divider, imageHeight / divider);
        final Random rnd = new Random(157);
        for (IRectanglesUnion.Frame frame : union.frames()) {
            draw(result, frame.rectangle(), divider, new Color(0, 0, 128), Color.BLUE);
        }
        for (IRectanglesUnion.BoundaryLink link : union.allHorizontalBoundaryLinks()) {
            draw(result, link.equivalentRectangle(), divider,
                new Color(0, 155 + rnd.nextInt(100), 0), Color.BLACK, 1);
        }
        for (IRectanglesUnion.BoundaryLink link : union.allVerticalBoundaryLinks()) {
            draw(result, link.equivalentRectangle(), divider,
                new Color(155 + rnd.nextInt(100), 0, 0), Color.BLACK, 0);
        }
        return result;
    }

    private static List<Matrix<? extends UpdatablePArray>> drawRectangles(
        long imageWidth, long imageHeight,
        IRectanglesUnion union,
        long divider)
    {
        final List<Matrix<? extends UpdatablePArray>> result = newImage(
                imageWidth / divider, imageHeight / divider);
        union.largestRectangleInUnion();
        // - force calling this method before access to private fields
        final Random rnd = new Random(157);
        for (IRectanglesUnion.Frame frame : union.frames()) {
            draw(result, frame.rectangle(), divider, new Color(0, 0, 128), Color.BLUE);
        }
        try {
            final Field horizontalSectionsByLowerSidesField =
                union.getClass().getDeclaredField("horizontalSectionsByLowerSides");
            horizontalSectionsByLowerSidesField.setAccessible(true);
            Collection<?> sections = (Collection<?>) horizontalSectionsByLowerSidesField.get(union);
            for (Object o : sections) {
                final IRectanglesUnion.Side section = (IRectanglesUnion.Side) o;
                final IRectangularArea largestRectangle;
                final Field largestRectangleField = section.getClass().getDeclaredField("largestRectangle");
                largestRectangleField.setAccessible(true);
                largestRectangle = (IRectangularArea) largestRectangleField.get(section);
                draw(result, largestRectangle, divider, Color.RED, new Color(155 + rnd.nextInt(100), 0, 0), 0);
                final IRectangularArea rectangle = section.equivalentRectangle();
                assert rectangle != null;
                draw(result, rectangle, divider, new Color(255, 155 + rnd.nextInt(100), 0), Color.BLACK);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unknown implementation: " + e);
        }
        draw(result, union.largestRectangleInUnion(), divider, Color.GREEN, Color.WHITE);
        return result;
    }

    private static List<Matrix<? extends UpdatablePArray>> drawBoundary(
        long imageWidth, long imageHeight,
        List<IRectanglesUnion.BoundaryLink> boundary,
        long divider,
        boolean precise)
    {
        final List<Matrix<? extends UpdatablePArray>> result = newImage(
                imageWidth / divider, imageHeight / divider);
        int value = 256;
        final List<IPoint> vertices = precise ?
            IRectanglesUnion.boundaryVerticesPlusHalf(boundary) :
            IRectanglesUnion.boundaryVerticesAtRectangles(boundary);
        for (int k = 0; k < boundary.size(); k++) {
            IRectanglesUnion.BoundaryLink link = boundary.get(k);
            if (value >= 256) {
                value = 63;
            }
            IPoint v1, v2;
            v1 = vertices.get(k);
            v2 = vertices.get(k == boundary.size() - 1 ? 0 : k + 1);
            draw(result, IRectangularArea.of(v1.min(v2), v1.max(v2)),
                    divider, new Color(value, 0, 0), null);
            if (!precise) {
                draw(result, link.equivalentRectangle(), divider, new Color(value, value, 0), null);
            }
            value += 32;
        }
        return result;
    }

    private static void draw(
        List<Matrix<? extends UpdatablePArray>> demo,
        IRectangularArea area,
        long divider,
        Color borderColor,
        Color innerColor)
    {
        draw(demo, area, divider, borderColor, innerColor, null);
    }

    private static void draw(
        List<Matrix<? extends UpdatablePArray>> demo,
        IRectangularArea area,
        long divider,
        Color borderColor,
        Color innerColor,
        Integer chosenColorComponent)
    {
        if (area == null) {
            return;
        }
        final IRectangularArea divided = IRectangularArea.of(
            area.min().multiply(1.0 / divider),
            area.max().multiply(1.0 / divider));
        for (int k = chosenColorComponent == null ? 0 : chosenColorComponent;
             k < (chosenColorComponent == null ? demo.size() : chosenColorComponent + 1);
             k++) {
            int borderValue = k == 0 ? borderColor.getRed() : k == 1 ? borderColor.getGreen() : borderColor.getBlue();
            demo.get(k).subMatrix(divided, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(borderValue);
            if (divided.size(0) > 2 && divided.size(1) > 2) {
                int innerValue = k == 0 ? innerColor.getRed() : k == 1 ? innerColor.getGreen() : innerColor.getBlue();
                final IRectangularArea inner = IRectangularArea.of(
                    divided.min().addToAllCoordinates(1),
                    divided.max().addToAllCoordinates(-1));
                demo.get(k).subMatrix(inner, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(innerValue);
            }
        }
    }
}
