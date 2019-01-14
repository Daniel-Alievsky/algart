/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

// More efficient analog of HorizontalIBracketSet for a case of horizontal boundary links.
// Really it can be used for vertical links also.
// Unlike HorizontalIBracketSet, this class does not support onlyStrictIntersections mode.
class HorizontalBoundaryIBracketSet<L extends IRectanglesUnion.BoundaryLink> {
    private final List<L> allHorizontals;
    private final int numberOfHorizontals;
    int horizontalIndex;
    L horizontal;
    long coord;
    private NavigableSet<Integer> intersectingLinks = new TreeSet<Integer>();
    // - index if x-coordinates array for of all points, where the current horizontal goes out of the figure

    public HorizontalBoundaryIBracketSet(List<L> allHorizontals) {
        assert allHorizontals != null;
        assert !allHorizontals.isEmpty();
        // - checked in the calling method to simplify the logic
        this.allHorizontals = allHorizontals;
        this.numberOfHorizontals = allHorizontals.size();
        this.horizontalIndex = -1;
        this.horizontal = null;
        if (IRectanglesUnion.DEBUG_LEVEL >= 2) {
            for (int k = 1, n = allHorizontals.size(); k < n; k++) {
                assert allHorizontals.get(k - 1).coord() <= allHorizontals.get(k).coord();
            }
        }
    }

    public boolean next() {
        if (horizontalIndex == numberOfHorizontals) {
            throw new IllegalArgumentException(getClass() + " should not be used more");
        }
        assert horizontalIndex < numberOfHorizontals;
        final L newHorizontal = horizontalIndex + 1 < numberOfHorizontals ?
            allHorizontals.get(horizontalIndex + 1) :
            null;
        // Theoretically, if it is null, we may just return false and do not anything; but we prefer
        // to remove the last brackets for self-testing goals (intersectingSides must become empty)
        final long newCoord = newHorizontal == null ? -157 : newHorizontal.coord();
        if (horizontal == null || newHorizontal == null
            || newHorizontal.coord() != horizontal.coord())
        {
            L h;
            if (horizontal != null) {
                int index = horizontalIndex;
                while (index >= 0 && (h = allHorizontals.get(index)).coord() == coord) {
                    removeHorizontal(h);
                    index--;
                }
            }

            if (newHorizontal != null) {
                int index = horizontalIndex + 1;
                while (index < numberOfHorizontals && (h = allHorizontals.get(index)).coord() == newCoord) {
                    addHorizontal(h);
                    index++;
                }
            }
        }
        assert intersectingLinks.size() % 2 == 0;
        horizontalIndex++;
        horizontal = newHorizontal;
        coord = newCoord;
        if (newHorizontal == null && !intersectingLinks.isEmpty()) {
            throw new AssertionError("Non-empty intersection set at the end of the loop");
        }
        if (IRectanglesUnion.DEBUG_LEVEL >= 3) {
            IRectanglesUnion.debug(3, "  Horizontal #%d, y=%d%s, %s; brackets: %s%n",
                horizontalIndex, coord,
                horizontal == null ? " (LOOP FINISHED)" :
                    horizontal.atFirstOfTwoParallelSides() ? " (starting)" : " (ending)",
                horizontal,
                intersectingLinks);
        }
        return horizontal != null;
    }

    public int maxLeftIndexBeloningToUnion() {
        // The boundary structure is always very simple: even interection with it (#0,#2,#4,...)
        // enter into the figure, odd intersections (#1,#3,#5,...) leave it
        final int fromIndex = horizontal.linkFrom().parentSeries.numberOfLessCoordinatesAtBoundary;
        assert fromIndex >= 0;
        final NavigableSet<Integer> headSet = intersectingLinks.headSet(fromIndex, false);
        if (headSet.size() % 2 == 0) {
            return fromIndex;
        } else {
            return headSet.last();
        }
    }

    public int minRightIndexBeloningToUnion() {
        final int toIndex = horizontal.linkTo().parentSeries.numberOfLessCoordinatesAtBoundary;
        assert toIndex >= 0;
        final NavigableSet<Integer> tailSet = intersectingLinks.tailSet(toIndex, false);
        if (tailSet.size() % 2 == 0) {
            return toIndex;
        } else {
            return tailSet.first();
        }
    }

    private void addHorizontal(IRectanglesUnion.BoundaryLink h) {
        if (h.atFirstOfTwoParallelSides()) {
            final IRectanglesUnion.BoundaryLink linkFrom = h.linkFrom();
            final IRectanglesUnion.BoundaryLink linkTo = h.linkTo();
            final boolean lessFrom = linkFrom.linkTo() == h;
            final boolean lessTo = linkTo.linkTo() == h;
            assert lessFrom || linkFrom.linkFrom() == h;
            assert lessTo || linkTo.linkFrom() == h;
            if (!lessFrom) {
                if (!lessTo) {
                    //     |xxxx|      y=161
                    //     |xxxx|      y=160
                    //     ------      y=159
                    //                 y=158
                    addBracket(linkFrom);
                    addBracket(linkTo);
                } else {
                    //     |xxxxxxx
                    //     |xxxxxxx
                    //     |-----xx
                    //          |xx
                    //          |xx
                    addBracket(linkFrom);
                    removeBracket(linkTo);
                }
            } else {
                if (!lessTo) {
                    //   xxxxxxx|
                    //   xxxxxxx|
                    //   xx------
                    //   xx|
                    //   xx|
                    addBracket(linkTo);
                    removeBracket(linkFrom);
                } else {
                    //  xxxxxxxxxxx
                    //  xxxxxxxxxxx
                    //  xx ------xx
                    //  xx |    |xx
                    //  xx |    |xx
                    removeBracket(linkFrom);
                    removeBracket(linkTo);
                }
            }
        }
    }

    private void removeHorizontal(IRectanglesUnion.BoundaryLink h) {
        if (h.atSecondOfTwoParallelSides()) {
            final IRectanglesUnion.BoundaryLink linkFrom = h.linkFrom();
            final IRectanglesUnion.BoundaryLink linkTo = h.linkTo();
            final boolean lessFrom = linkFrom.linkTo() == h;
            final boolean lessTo = linkTo.linkTo() == h;
            assert lessFrom || linkFrom.linkFrom() == h;
            assert lessTo || linkTo.linkFrom() == h;
            if (lessFrom) {
                if (lessTo) {
                    //                 y=160
                    //     ------      y=159
                    //     |xxxx|      y=158
                    //     |xxxx|      y=157
                    removeBracket(linkFrom);
                    removeBracket(linkTo);
                } else {
                    //          |xx
                    //          |xx
                    //     ------xx
                    //     |xxxxxxx
                    //     |xxxxxxx
                    removeBracket(linkFrom);
                    addBracket(linkTo);
                }
            } else {
                if (lessTo) {
                    //   xx|
                    //   xx|
                    //   xx|-----
                    //   xxxxxxx|
                    //   xxxxxxx|
                    removeBracket(linkTo);
                    addBracket(linkFrom);
                } else {
                    //   xx|    |xx
                    //   xx|    |xx
                    //   xx|-----xx
                    //   xxxxxxxxxx
                    //   xxxxxxxxxx
                    addBracket(linkFrom);
                    addBracket(linkTo);
                }
            }
        }
    }

    private void addBracket(IRectanglesUnion.BoundaryLink link) {
        assert link.parentSeries.indexInSortedListAtBoundary >= 0 :
            "indexInSortedListAtBoundary is not set in " + link.parentSeries;
        final boolean newlyAdded = intersectingLinks.add(link.parentSeries.numberOfLessCoordinatesAtBoundary);
        assert newlyAdded;
    }

    private void removeBracket(IRectanglesUnion.BoundaryLink link) {
        assert link.parentSeries.indexInSortedListAtBoundary >= 0 :
            "indexInSortedListAtBoundary is not set in " + link.parentSeries;
        final boolean contained = intersectingLinks.remove(link.parentSeries.numberOfLessCoordinatesAtBoundary);
        assert contained;
    }

}
