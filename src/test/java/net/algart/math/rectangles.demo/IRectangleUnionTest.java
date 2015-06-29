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

package net.algart.math.rectangles.demo;

import net.algart.arrays.Arrays;
import net.algart.arrays.ExternalProcessor;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatablePArray;
import net.algart.external.ExternalAlgorithmCaller;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.math.rectangles.IRectanglesUnion;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IRectangleUnionTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage:");
            System.out.println("    " + IRectangleUnionTest.class.getName()
                + " numberOfTests rectangles-description.txt demo-files-folder [coordinate-divider]");
            return;
        }
        final int numberOfTests = Integer.parseInt(args[0]);
        final File rectanglesFile = new File(args[1]);
        final File demoFolder = new File(args[2]);
        final double coordinateDivider;
        demoFolder.mkdirs();
        final String[] description = ExternalProcessor.readUTF8(rectanglesFile).split("[\\r\\n]+");
        for (int k = 0; k < description.length; k++) {
            final int p = description[k].indexOf("//");
            if (p != -1) {
                description[k] = description[k].substring(0, p);
            }
            description[k] = description[k].trim();
        }
        final String algorithm = description[0];
        Long imageWidth = null;
        Long imageHeight = null;
        final List<IRectangularArea> rectangles = new ArrayList<IRectangularArea>();
        if (algorithm.equals("rectangles")) {
            System.out.println("Reading rectangles...");
            coordinateDivider = args.length >= 4 ? Double.parseDouble(args[3]) : 1.0;
            for (int k = 1; k < description.length; k++) {
                final String[] rectangle = description[k].split("[\\s,]+");
                rectangles.add(IRectangularArea.valueOf(
                    IPoint.valueOf(
                        Integer.parseInt(rectangle[0]),
                        Integer.parseInt(rectangle[1])),
                    IPoint.valueOf(
                        Integer.parseInt(rectangle[2]),
                        Integer.parseInt(rectangle[3]))));
            }
        } else {
            System.out.println("Creating rectangles...");
            coordinateDivider = 1.0;
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
                imageWidth = Long.parseLong(description[5]);
                imageHeight = Long.parseLong(description[6]);
                if (imageWidth <= 0) {
                    imageWidth = null;
                }
                if (imageHeight <= 0) {
                    imageHeight = null;
                }
                final int horizontalCount = Integer.parseInt(description[7]);
                final int verticalCount = Integer.parseInt(description[8]);
                final int overlap = Integer.parseInt(description[9]);
                final int maxError = Integer.parseInt(description[10]);
                final int randSeed = Integer.parseInt(description[11]);
                final Random rnd = randSeed == -1 ? new Random() : new Random(randSeed);
                for (int i = 0; i < verticalCount; i++) {
                    for (int j = 0; j < horizontalCount; j++) {
                        final int frameWidth = frameMinWidth + rnd.nextInt(frameMaxWidth - frameMinWidth + 1);
                        final int frameHeight = frameMinHeight + rnd.nextInt(frameMaxHeight - frameMinHeight + 1);
                        final long x = (j + 1) * (frameWidth - overlap) + rnd.nextInt(maxError + 1) - maxError / 2;
                        final long y = (i + 1) * (frameHeight - overlap) + rnd.nextInt(maxError + 1) - maxError / 2;
                        final IRectangularArea r = IRectangularArea.valueOf(
                            x, y, x + frameWidth - 1, y + frameHeight - 1);
                        if (rectangles.size() < 10) {
                            System.out.printf("Frame #%d %dx%d: %s%n", rectangles.size(), r.size(0), r.size(1), r);
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
        IRectanglesUnion rectangleUnion = IRectanglesUnion.newInstance(rectangles);
        if (imageWidth == null) {
            imageWidth = rectangleUnion.circumscribedRectangle().max(0) + 100;
        }
        if (imageHeight == null) {
            imageHeight = rectangleUnion.circumscribedRectangle().max(1) + 100;
        }
        List<Matrix<? extends UpdatablePArray>> demo = newImage(imageWidth, imageHeight);
        draw(demo, rectangleUnion.circumscribedRectangle(), coordinateDivider, Color.YELLOW, Color.BLUE);
        for (IRectangularArea area : rectangles) {
            draw(demo, area, coordinateDivider, Color.LIGHT_GRAY, Color.DARK_GRAY);
        }
        final File sourceFile = new File(demoFolder, rectanglesFile.getName() + ".source.bmp");
        System.out.printf("Writing source image %dx%d into %s: %d rectangles%n",
            imageWidth, imageHeight, sourceFile, rectangles.size());
        ExternalAlgorithmCaller.writeImage(sourceFile, demo);

        for (int testIndex = 0; testIndex < numberOfTests; testIndex++) {
            System.out.printf("%nTest #%d%n", testIndex + 1);
            rectangleUnion = IRectanglesUnion.newInstance(rectangles);
            rectangleUnion.findConnectedComponents();
            final Random rnd = new Random(157);
            for (int k = 0; k < Math.min(10, rectangleUnion.connectedComponentCount()); k++) {
                demo = newImage(imageWidth, imageHeight);
                final IRectanglesUnion connectedSet = rectangleUnion.connectedComponent(k);
                connectedSet.findBoundaries();
                if (testIndex == 0) {
                    for (IRectanglesUnion.Frame frame : connectedSet.frames()) {
                        draw(demo, frame.rectangle(), coordinateDivider, Color.DARK_GRAY, Color.BLUE);
                    }
                    final List<IRectanglesUnion.HorizontalBoundaryLink> horizontals =
                        connectedSet.allHorizontalBoundaryLinks();
                    for (IRectanglesUnion.BoundaryLink link : horizontals) {
                        draw(demo, link.sidePart(), coordinateDivider,
                            new Color(0, 200 + rnd.nextInt(55), 0), Color.BLACK, 1);
                    }
                    final List<IRectanglesUnion.VerticalBoundaryLink> verticals =
                        connectedSet.allVerticalBoundaryLinks();
                    for (IRectanglesUnion.BoundaryLink link : verticals) {
                        draw(demo, link.sidePart(), coordinateDivider,
                            new Color(200 + rnd.nextInt(55), 0, 0), Color.BLACK, 0);
                    }
                    final File f = new File(demoFolder, rectanglesFile.getName() + ".component" + k + ".bmp");
                    System.out.printf("Writing component #%d into %s: %s; "
                        + "%d horizontal and %d vertical boundary links %n%n",
                        k, f, connectedSet, horizontals.size(), verticals.size());
                    ExternalAlgorithmCaller.writeImage(f, demo);
                }
            }
        }
    }

    private static List<Matrix<? extends UpdatablePArray>> newImage(long width, long height) {
        ArrayList<Matrix<? extends UpdatablePArray>> result = new ArrayList<Matrix<? extends UpdatablePArray>>();
        result.add(Arrays.SMM.newByteMatrix(width, height));
        result.add(Arrays.SMM.newByteMatrix(width, height));
        result.add(Arrays.SMM.newByteMatrix(width, height));
        return result;
    }

    private static void draw(
        List<Matrix<? extends UpdatablePArray>> demo,
        IRectangularArea area,
        double coordinateDivider,
        Color borderColor,
        Color innerColor)
    {
        draw(demo, area, coordinateDivider, borderColor, innerColor, null);
    }
    private static void draw(
        List<Matrix<? extends UpdatablePArray>> demo,
        IRectangularArea area,
        double coordinateDivider,
        Color borderColor,
        Color innerColor,
        Integer chosenColorComponent)
    {
        final IRectangularArea divided = IRectangularArea.valueOf(
            area.min().multiply(1.0 / coordinateDivider),
            area.max().multiply(1.0 / coordinateDivider));
        for (int k = chosenColorComponent == null ? 0 : chosenColorComponent;
             k < (chosenColorComponent == null ? demo.size() : chosenColorComponent + 1);
             k++)
        {
            int borderValue = k == 0 ? borderColor.getRed() : k == 1 ? borderColor.getGreen() : borderColor.getBlue();
            int innerValue = k == 0 ? innerColor.getRed() : k == 1 ? innerColor.getGreen() : innerColor.getBlue();
            demo.get(k).subMatrix(divided, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(borderValue);
            if (divided.size(0) > 2 && divided.size(1) > 2) {
                final IRectangularArea inner = IRectangularArea.valueOf(
                    divided.min().addToAllCoordinates(1),
                    divided.max().addToAllCoordinates(-1));
                demo.get(k).subMatrix(inner, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(innerValue);
            }
        }
    }
}
