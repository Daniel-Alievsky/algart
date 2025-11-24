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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.math.IRectangularArea;
import net.algart.math.RectangularArea;

import java.util.*;

public class SubtractRectangleTest {
    private final Random rnd = new Random(157);
    private int width = 1000;
    private int height = 1000;
    private Matrix<UpdatableBitArray> m1, m2, m3;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Usage: %s maxNumberOfRectangles numberOfTests [width height]%n",
                    SubtractRectangleTest.class.getName());
            return;
        }
        SubtractRectangleTest test = new SubtractRectangleTest();
        int numberOfRectangles = Integer.parseInt(args[0]);
        int numberOfTests = Integer.parseInt(args[1]);
        if (args.length > 2) {
            test.width = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            test.height = Integer.parseInt(args[3]);
        }
        test.initialize();
        System.out.printf("Testing %d rectangles %d times at %dx%d...%n",
                numberOfRectangles, numberOfTests, test.width, test.height);
        for (int textIndex = 1; textIndex <= numberOfTests; textIndex++) {
            System.out.printf("Test #%d/%d...\r", textIndex, numberOfTests);
            List<IRectangularArea> fromWhatToSubtract = test.makeRandom(numberOfRectangles);
            List<IRectangularArea> whatToSubtract = test.makeRandom(numberOfRectangles);
            test.testRectangles(fromWhatToSubtract, whatToSubtract);
        }
    }

    private void initialize() {
        m1 = Matrix.newBitMatrix(width, height);
        m2 = Matrix.newBitMatrix(width, height);
        m3 = Matrix.newBitMatrix(width, height);
    }

    private List<IRectangularArea> makeRandom(int maxNumberOfRectangles) {
        if (rnd.nextInt(3) == 0) {
            return List.of(IRectangularArea.ofSize(0, 0, width, height));
        }
        int number = rnd.nextInt(maxNumberOfRectangles + 1);
        final ArrayList<IRectangularArea> result = new ArrayList<>();
        for (int k = 0; k < number; k++) {
            result.add(makeRandomRectangle());
        }
        return result;
    }

    private IRectangularArea makeRandomRectangle() {
        int x = rnd.nextInt(width);
        int y = rnd.nextInt(height);
        int w = rnd.nextInt(width - x) + 1;
        int h = rnd.nextInt(height - y) + 1;
        return IRectangularArea.ofSize(x, y, w, h);
    }

    private void testRectangles(List<IRectangularArea> fromWhatToSubtract, List<IRectangularArea> whatToSubtract) {
        List<IRectangularArea> result = testSubtraction(fromWhatToSubtract, whatToSubtract);
        testFloatingPoint(fromWhatToSubtract, whatToSubtract, result);
    }

    private List<IRectangularArea> testSubtraction(
            List<IRectangularArea> fromWhatToSubtract,
            List<IRectangularArea> whatToSubtract) {
        draw(m1, fromWhatToSubtract);
        draw(m2, whatToSubtract);
        Matrices.bitAndNot(m1, m2);
        LinkedList<IRectangularArea> result = new LinkedList<>(fromWhatToSubtract);
        IRectangularArea.subtractCollection(result, whatToSubtract);
//        System.out.printf("%s%n - %s%n = %s%n", fromWhatToSubtract, whatToSubtract, result);
        draw(m3, result);
        if (!m1.equals(m3)) {
            throw new AssertionError("Error found!\n" + fromWhatToSubtract +
                    "\n -" + whatToSubtract + "\n = " + result);
        }
        return result;
    }

    private void testFloatingPoint(
            List<IRectangularArea> fromWhatToSubtract,
            List<IRectangularArea> whatToSubtract,
            List<IRectangularArea> correctResult) {
        List<RectangularArea> fromWhat = fromWhatToSubtract.stream().map(IRectangularArea::toContainingArea).toList();
        List<RectangularArea> what = whatToSubtract.stream().map(IRectangularArea::toContainingArea).toList();
        LinkedList<RectangularArea> result = new LinkedList<>(fromWhat);
        RectangularArea.subtractCollection(result, what);
        draw(m1, correctResult);
        drawFloatingPoint(m3, result);
        if (!m1.equals(m3)) {
            throw new AssertionError("Floating-point error found!\n" + fromWhatToSubtract +
                    "\n -" + whatToSubtract + "\n = " + result + "\ninstead of\n   " + correctResult);
        }
    }

    private void draw(Matrix<UpdatableBitArray> m, Collection<IRectangularArea> rectangles) {
        m.array().fill(false);
        for (IRectangularArea r : rectangles) {
            m.subMatrix(r, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(true);
        }
    }

    private void drawFloatingPoint(Matrix<UpdatableBitArray> m, Collection<RectangularArea> rectangles) {
        m.array().fill(false);
        for (RectangularArea r : rectangles) {
            if (r.volume() > 0.0) {
                IRectangularArea rectangle = IRectangularArea.of(
                        r.min().toRoundedPoint(),
                        r.max().toRoundedPoint().addToAllCoordinates(-1));
                m.subMatrix(rectangle, Matrix.ContinuationMode.NULL_CONSTANT).array().fill(true);
            }
        }
    }
}
