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

package net.algart.math.rectangles;

import java.util.*;

// This class should be used in a single thread.
class HorizontalIBracketSet {
    private final List<IRectanglesUnion.HorizontalSide> allHorizontalSides;
    private final int numberOfHorizontals;
    private boolean onlyStrictIntersections;
    int horizontalIndex;
    IRectanglesUnion.HorizontalSide horizontal;
    long y;
    private NavigableSet<IBracket> intersectingSides = new TreeSet<IBracket>();

    HorizontalIBracketSet(List<IRectanglesUnion.HorizontalSide> allHorizontalSides, boolean onlyStrictIntersections) {
        assert allHorizontalSides != null;
        assert !allHorizontalSides.isEmpty();
        // - checked in the calling method to simplify the logic
        this.allHorizontalSides = allHorizontalSides;
        this.numberOfHorizontals = allHorizontalSides.size();
        this.onlyStrictIntersections = onlyStrictIntersections;
        this.horizontalIndex = -1;
        this.horizontal = null;
        if (IRectanglesUnion.DEBUG_LEVEL >= 2) {
            for (int k = 1, n = allHorizontalSides.size(); k < n; k++) {
                assert allHorizontalSides.get(k).boundCoord() >= allHorizontalSides.get(k - 1).boundCoord();
            }
        }
    }

    boolean next() {
        if (horizontalIndex == numberOfHorizontals) {
            throw new IllegalArgumentException(getClass() + " should not be used more");
        }
        assert horizontalIndex < numberOfHorizontals;
        final IRectanglesUnion.HorizontalSide newHorizontal = horizontalIndex + 1 < numberOfHorizontals ?
            allHorizontalSides.get(horizontalIndex + 1) :
            null;
        // Theoretically, if it is null, we may just return false and do not anything; but we prefer
        // to remove the last brackets for self-testing goals (intersectingSides must become empty)
        final long newY = newHorizontal == null ? -157 : newHorizontal.boundCoord();
        if (onlyStrictIntersections) {
            if (horizontal == null || newHorizontal == null
                || newHorizontal.boundCoord() != horizontal.boundCoord()
                || newHorizontal.first != horizontal.first)
            {
                IRectanglesUnion.HorizontalSide h;
                if (horizontal != null) {
                    int index = horizontalIndex;
                    while (index >= 0
                        && (h = allHorizontalSides.get(index)).boundCoord() == y
                        && h.first == horizontal.first) {
                        if (h.isFirstOfTwoParallelSides()) {
                            addHorizontal(h);
                        }
                        index--;
                    }
                }
                if (newHorizontal != null) {
                    int index = horizontalIndex + 1;
                    while (index < numberOfHorizontals
                        && (h = allHorizontalSides.get(index)).boundCoord() == newY
                        && h.first == newHorizontal.first) {
                        if (h.isSecondOfTwoParallelSides()) {
                            removeHorizontal(h);
                        }
                        index++;
                    }
                }
            }
        } else {
            if (horizontal == null || newHorizontal == null
                || newHorizontal.boundCoord() != horizontal.boundCoord())
            {
                IRectanglesUnion.HorizontalSide h;
                if (horizontal != null) {
                    int index = horizontalIndex;
                    while (index >= 0 && (h = allHorizontalSides.get(index)).boundCoord() == y) {
                        if (h.isSecondOfTwoParallelSides()) {
                            removeHorizontal(h);
                        }
                        index--;
                    }
                }

                if (newHorizontal != null) {
                    int index = horizontalIndex + 1;
                    while (index < numberOfHorizontals && (h = allHorizontalSides.get(index)).boundCoord() == newY) {
                        if (h.isFirstOfTwoParallelSides()) {
                            addHorizontal(h);
                        }
                        index++;
                    }
                }
            }
        }
        horizontalIndex++;
        horizontal = newHorizontal;
        y = newY;
        if (newHorizontal == null && !intersectingSides.isEmpty()) {
            throw new AssertionError("Non-empty intersection set at the end of the loop");
        }
        if (IRectanglesUnion.DEBUG_LEVEL >= 3) {
            System.out.printf("  Horizontal #%d, y=%d%s; brackets:%s",
                horizontalIndex, y,
                horizontal == null ? " (LOOP FINISHED)" : horizontal.first ? "(starting)" : " (ending)",
                toDebugString(intersectingSides));
        }
        return horizontal != null;
    }

    NavigableSet<IBracket> currentIntersections() {
        final IBracket bracketFrom = new IBracket(horizontal, horizontal.frame.lessVerticalSide, true);
        final IBracket bracketTo = new IBracket(horizontal, horizontal.frame.higherVerticalSide, false);
        if (IRectanglesUnion.DEBUG_LEVEL >= 1 && !onlyStrictIntersections) {
            assert intersectingSides.contains(bracketFrom);
            assert intersectingSides.contains(bracketTo);
        }
        final NavigableSet<IBracket> result = intersectingSides.subSet(bracketFrom, true, bracketTo, true);
        if (IRectanglesUnion.DEBUG_LEVEL >= 3) {
            System.out.printf("  Intersections with %s; brackets:%s", horizontal, toDebugString(result));
        }
        return result;
    }

    IBracket lastIntersectionBeforeLeft() {
        final IBracket bracketFrom = new IBracket(horizontal, horizontal.frame.lessVerticalSide, true);
        return intersectingSides.lower(bracketFrom);
    }

    IBracket firstIntersectionAfterRight() {
        final IBracket bracketTo = new IBracket(horizontal, horizontal.frame.higherVerticalSide, true);
        return intersectingSides.higher(bracketTo);
    }

    private void addHorizontal(IRectanglesUnion.HorizontalSide h) {
        final IBracket bracketFrom = new IBracket(h, h.frame.lessVerticalSide, true);
        final IBracket bracketTo = new IBracket(h, h.frame.higherVerticalSide, false);
        // Note: theoretically it could be faster not to allocate brackets here,
        // but create them together with Side instance and store there as its fields.
        // But it is a bad idea, because can lead to problems with multithreading when
        // two threads modify Bracket.rightNestingDepth fields.
        // In any case, this solution requires comparable time for allocation
        // while the single pass of the scanning by the horizontal.
        final IBracket previousBracket = intersectingSides.lower(bracketFrom);
        int nesting = previousBracket == null ? 1 : previousBracket.followingCoveringDepth + 1;
        bracketFrom.followingCoveringDepth = nesting;
        for (IBracket bracket : intersectingSides.subSet(bracketFrom, false, bracketTo, false)) {
            // It is not the ideal O(log N) algorithm, but close to it, because this subset is usually very little
            nesting = ++bracket.followingCoveringDepth;
        }
        bracketTo.followingCoveringDepth = nesting - 1;
        intersectingSides.add(bracketFrom);
        intersectingSides.add(bracketTo);
    }

    private void removeHorizontal(IRectanglesUnion.HorizontalSide h) {
        final IBracket bracketFrom = new IBracket(h, h.frame.lessVerticalSide, true);
        final IBracket bracketTo = new IBracket(h, h.frame.higherVerticalSide, false);
        for (IBracket bracket : intersectingSides.subSet(bracketFrom, false, bracketTo, false)) {
            --bracket.followingCoveringDepth;
        }
        final boolean containedFrom = intersectingSides.remove(bracketFrom);
        final boolean containedTo = intersectingSides.remove(bracketTo);
        assert containedFrom;
        assert containedTo;
    }

    private static String toDebugString(Collection<IBracket> brackets) {
        if (brackets.isEmpty()) {
            return String.format(" NONE%n");
        }
        StringBuilder sb = new StringBuilder(String.format("%n"));
        for (IBracket bracket : brackets) {
            sb.append(String.format("    %s%n", bracket));
        }
        return sb.toString();
    }

}
