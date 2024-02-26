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

package net.algart.math.rectangles;

class IBracket implements Comparable<IBracket> {
    final IRectanglesUnion.FrameSide intersectingSide;
    final long coord;
    final boolean first;
    int followingCoveringDepth = -157;

    public IBracket(IRectanglesUnion.FrameSide intersectingSide, boolean first) {
        this.intersectingSide = intersectingSide;
        this.coord = intersectingSide.coord();
        this.first = first;
        assert intersectingSide.first == first;
        // - built-in additional check
    }

    public boolean covers(long transversalCoord) {
        return intersectingSide.from() <= transversalCoord && intersectingSide.to() >= transversalCoord;
    }

    @Override
    public int compareTo(IBracket o) {
        if (this.coord < o.coord) {
            return -1;
        }
        if (this.coord > o.coord) {
            return 1;
        }
        if (this.first && !o.first) {
            return -1;
        }
        if (!this.first && o.first) {
            return 1;
        }
        // We need some unique identifier to allow storing in TreeSet several brackets
        // with the same x (from different frames):
        final int thisUniqueId = this.intersectingSide.frame.index;
        final int otherUniqueId = o.intersectingSide.frame.index;
        if (thisUniqueId < otherUniqueId) {
            return -1;
        }
        if (thisUniqueId > otherUniqueId) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return (first ? "opening" : "closing") + " bracket " + coord
            + ", covering level after it: " + followingCoveringDepth;
    }
}
