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

import net.algart.math.IRectangularArea;

import java.util.*;
import java.util.Arrays;

public class IRectanglesUnion {
    static final int DEBUG_LEVEL = net.algart.arrays.Arrays.SystemSettings.getIntProperty(
        "net.algart.math.rectangles.debugLevel", 0);

    public static class Frame {
        final HorizontalSide lessHorizontalSide;
        final HorizontalSide higherHorizontalSide;
        final VerticalSide lessVerticalSide;
        final VerticalSide higherVerticalSide;
        private final IRectangularArea rectangle;
        private final long fromX;
        private final long toX;
        private final long fromY;
        private final long toY;
        final int index;

        private Frame(IRectangularArea rectangle, int index) {
            assert rectangle != null;
            this.rectangle = rectangle;
            this.lessHorizontalSide = new HorizontalSide(true, this);
            this.higherHorizontalSide = new HorizontalSide(false, this);
            this.lessVerticalSide = new VerticalSide(true, this);
            this.higherVerticalSide = new VerticalSide(false, this);
            this.fromX = rectangle.min(0);
            this.toX = rectangle.max(0) + 1;
            this.fromY = rectangle.min(1);
            this.toY = rectangle.max(1) + 1;
            this.index = index;
        }

        public IRectangularArea rectangle() {
            return rectangle;
        }
    }

    public static abstract class Side implements Comparable<Side> {
        final boolean first;

        private Side(boolean first) {
            this.first = first;
        }

        public boolean isFirstOfTwoParallelSides() {
            return first;
        }

        public boolean isSecondOfTwoParallelSides() {
            return !first;
        }

        public abstract boolean isHorizontal();

        public long frameSideCoord() {
            return isFirstOfTwoParallelSides() ? boundCoord() : boundCoord() - 1;
        }

        /**
         * Returns the coordinate of this frame side along the coordinate axis,
         * to which this side is perpendicular, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the perpendicular coordinate of this side + 0.5
         */
        public abstract long boundCoord();

        /**
         * Returns the starting coordinate of this frame side along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the starting coordinate of this side + 0.5
         */
        public abstract long boundFrom();

        /**
         * Returns the ending coordinate of this frame side along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the ending coordinate of this side + 0.5
         */
        public abstract long boundTo();


        @Override
        public int compareTo(Side o) {
            if (this.getClass() != o.getClass()) {
                throw new ClassCastException("Comparison of sides with different types: "
                    + getClass() + " != " + o.getClass());
            }
            final long thisCoord = this.boundCoord();
            final long otherCoord = o.boundCoord();
            if (thisCoord < otherCoord) {
                return -1;
            }
            if (thisCoord > otherCoord) {
                return 1;
            }
            // Lets we have two adjacent rectangles:
            // AAAAAAAAAAAA
            // AAAAAAAAAAAA
            // AAAAAAAAAAAA
            //      BBBBBBBBBBB
            //      BBBBBBBBBBB
            //      BBBBBBBBBBB
            // where the top side of B lies at the same horizontal as the bottom side of A.
            // The following checks provide the sorting order, when "opening" top side of B
            // will be BEFORE the "closing" bottom side of A:
            //      A top side (A start)
            //      B top side (B start)
            //      A bottom side (A finish)
            //      B bottom side (B finish)
            // It is important to consider such rectangles intersecting.
            // Note: the Bracket class uses REVERSE logic.
            if (this.first && !o.first) {
                return -1;
            }
            if (!this.first && o.first) {
                return 1;
            }
            // Sorting along another coordinate is necessary for searching several sides,
            // which are really a single continuous segment
            final long thisFrom = this.boundFrom();
            final long otherFrom = o.boundFrom();
            if (thisFrom < otherFrom) {
                return -1;
            }
            if (thisFrom > otherFrom) {
                return 1;
            }
            final long thisTo = this.boundTo();
            final long otherTo = o.boundTo();
            if (thisTo < otherTo) {
                return -1;
            }
            if (thisTo > otherTo) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return (isHorizontal() ? (first ? "top" : "bottom") : (first ? "left" : "right")) + " side"
                + (this instanceof FrameSide ? " of frame #" + ((FrameSide) this).frame.index : "")
                + ": " + boundFrom() + ".." + boundTo() + " at " + boundCoord();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Side side = (Side) o;
            if (first != side.first) {
                return false;
            }
            return this.boundCoord() == side.boundCoord()
                && this.boundFrom() == side.boundFrom()
                && this.boundTo() == side.boundTo();
        }

        @Override
        public int hashCode() {
            int result = (first ? 1 : 0);
            final long boundCoord = boundCoord();
            final long boundFrom = boundFrom();
            final long boundTo = boundTo();
            result = 31 * result + (int) (boundCoord ^ (boundCoord >>> 32));
            result = 31 * result + (int) (boundFrom ^ (boundFrom >>> 32));
            result = 31 * result + (int) (boundTo ^ (boundTo >>> 32));
            return result;
        }

        abstract Side sideFrom();

        abstract Side sideTo();

        abstract long uniqueId();
    }

    public static abstract class FrameSide extends Side {
        final Frame frame;
        volatile SideSeries containingSeries = null;
        // Must be volatile, because initialized further and maybe from a parallel thread

        private FrameSide(boolean first, Frame frame) {
            super(first);
            assert frame != null;
            this.frame = frame;
        }

        public Frame frame() {
            return frame;
        }

        abstract FrameSide sideFrom();

        abstract FrameSide sideTo();

        @Override
        long uniqueId() {
            return first ? frame.index : -frame.index - 1;
        }
    }

    public static class HorizontalSide extends FrameSide {
        private HorizontalSide(boolean first, Frame frame) {
            super(first, frame);
        }

        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public long frameSideCoord() {
            return first ? frame.fromY : frame.toY - 1;
        }

        @Override
        public long boundCoord() {
            return first ? frame.fromY : frame.toY;
        }

        @Override
        public long boundFrom() {
            return frame.fromX;
        }

        @Override
        public long boundTo() {
            return frame.toX;
        }

        @Override
        FrameSide sideFrom() {
            return frame.lessVerticalSide;
        }

        @Override
        FrameSide sideTo() {
            return frame.higherVerticalSide;
        }
    }

    public static class VerticalSide extends FrameSide {
        private VerticalSide(boolean first, Frame frame) {
            super(first, frame);
        }

        @Override
        public boolean isHorizontal() {
            return false;
        }

        @Override
        public long frameSideCoord() {
            return first ? frame.fromX : frame.toX - 1;
        }

        @Override
        public long boundCoord() {
            return first ? frame.fromX : frame.toX;
        }

        @Override
        public long boundFrom() {
            return frame.fromY;
        }

        @Override
        public long boundTo() {
            return frame.toY;
        }

        @Override
        FrameSide sideFrom() {
            return frame.lessVerticalSide;
        }

        @Override
        FrameSide sideTo() {
            return frame.higherVerticalSide;
        }
    }

    public static abstract class BoundaryLink implements Comparable<BoundaryLink> {
        final SideSeries containingSeries;
        final long from;
        final long to;

        private BoundaryLink(
            SideSeries containingSeries,
            long from,
            long to)
        {
            assert containingSeries != null;
            this.containingSeries = containingSeries;
            assert from <= to;
            this.from = from;
            this.to = to;
        }

        /**
         * Returns the coordinate of this boundary element (link) along the coordinate axis,
         * to which this link is perpendicular, increased by 0.5
         * (the bounrady always has half-integer coordinate).
         *
         * @return the perpendicular coordinate of this link + 0.5
         */
        public long coord() {
            return containingSeries.boundCoord();
        }

        /**
         * Returns the starting coordinate of this boundary element (link) along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the bounrady always has half-integer coordinate).
         *
         * @return the starting coordinate of this link + 0.5
         */
        public long from() {
            return from;
        }

        /**
         * Returns the ending coordinate of this boundary element (link) along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the bounrady always has half-integer coordinate).
         *
         * @return the ending coordinate of this link + 0.5
         */
        public long to() {
            return to;
        }

        public abstract IRectangularArea sidePart();

        @Override
        public int compareTo(BoundaryLink o) {
            if (this.coord() < o.coord()) {
                return -1;
            }
            if (this.coord() > o.coord()) {
                return 1;
            }
            if (this.from < o.from) {
                return -1;
            }
            if (this.from > o.from) {
                return 1;
            }
            // Further comparisons are not important for our algorithms:
            if (this.to < o.to) {
                return -1;
            }
            if (this.to > o.to) {
                return 1;
            }
            return this.containingSeries.compareTo(o.containingSeries);
        }

        @Override
        public String toString() {
            return (this instanceof HorizontalBoundaryLink ? "horizontal" : "vertical")
                + " boundary link " + from + ".." + to + " at " + containingSeries;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BoundaryLink that = (BoundaryLink) o;
            if (from != that.from) {
                return false;
            }
            if (to != that.to) {
                return false;
            }
            return this.containingSeries.equals(that.containingSeries);
        }

        @Override
        public int hashCode() {
            int result = containingSeries.hashCode();
            result = 31 * result + (int) (from ^ (from >>> 32));
            result = 31 * result + (int) (to ^ (to >>> 32));
            return result;
        }
    }

    private static class HorizontalBoundaryLink extends BoundaryLink {
        final SideSeries transversalSideFrom;
        final SideSeries transversalSideTo;
        // - these two fields are necessary only while constructing the boundary
        VerticalBoundaryLink linkFrom = null;
        VerticalBoundaryLink linkTo = null;

        private HorizontalBoundaryLink(
            SideSeries containingSeries,
            VerticalSideSeries transversalSideFrom,
            VerticalSideSeries transversalSideTo)
        {
            super(containingSeries,
                transversalSideFrom.boundCoord(),
                transversalSideTo.boundCoord());
            this.transversalSideFrom = transversalSideFrom;
            this.transversalSideTo = transversalSideTo;
        }

        @Override
        public IRectangularArea sidePart() {
            return IRectangularArea.valueOf(
                from, containingSeries.frameSideCoord(),
                to - 1, containingSeries.frameSideCoord());
        }

        void setNeighbour(VerticalBoundaryLink neighbour) {
            if (neighbour.containingSeries == transversalSideFrom) {
                linkFrom = neighbour;
            } else if (neighbour.containingSeries == transversalSideTo) {
                linkTo = neighbour;
            } else {
                throw new AssertionError("Attempt to assing vertical neighbour from alien side series");
            }
        }
    }

    public static class VerticalBoundaryLink extends BoundaryLink {
        final BoundaryLink linkFrom;
        final BoundaryLink linkTo;

        private VerticalBoundaryLink(
            SideSeries containingSeries,
            HorizontalBoundaryLink linkFrom,
            HorizontalBoundaryLink linkTo)
        {
            super(containingSeries, linkFrom.coord(), linkTo.coord());
            this.linkFrom = linkFrom;
            this.linkTo = linkTo;
        }

        @Override
        public IRectangularArea sidePart() {
            return IRectangularArea.valueOf(
                containingSeries.frameSideCoord(), from,
                containingSeries.frameSideCoord(), to - 1);
        }
    }


    private final List<Frame> frames;
    private final IRectangularArea circumscribedRectangle;

    private volatile List<HorizontalSide> horizontalSides = null;
    private volatile List<VerticalSide> verticalSides = null;
    private volatile List<HorizontalSideSeries> horizontalSideSeries = null;
    private volatile List<VerticalSideSeries> verticalSidesSeries = null;
    private volatile List<List<Frame>> connectedComponents = null;
    private volatile List<HorizontalSide> horizontalSidesAtBoundary = null;
    private volatile List<VerticalSide> verticalSidesAtBoundary = null;
    private volatile List<List<BoundaryLink>> allBoundaries = null;
    private final Object lock = new Object();

    IRectanglesUnion(List<Frame> frames) {
        this.frames = frames;
        if (frames.isEmpty()) {
            circumscribedRectangle = null;
        } else {
            long minX = Long.MAX_VALUE;
            long minY = Long.MAX_VALUE;
            long maxX = Long.MIN_VALUE;
            long maxY = Long.MIN_VALUE;
            for (Frame frame : frames) {
                minX = Math.min(minX, frame.fromX);
                minY = Math.min(minY, frame.fromY);
                maxX = Math.max(maxX, frame.toX - 1);
                maxY = Math.max(maxY, frame.toY - 1);
            }
            circumscribedRectangle = IRectangularArea.valueOf(minX, minY, maxX, maxY);
        }
    }

    public static IRectanglesUnion newInstance(Collection<IRectangularArea> rectangles) {
        return new IRectanglesUnion(checkAndConvertToFrames(rectangles));
    }

    public List<Frame> frames() {
        return Collections.unmodifiableList(frames);
    }

    public IRectangularArea circumscribedRectangle() {
        return circumscribedRectangle;
    }

    public List<HorizontalSide> horizontalSides() {
        return Collections.unmodifiableList(horizontalSides);
    }

    public List<VerticalSide> verticalSides() {
        return Collections.unmodifiableList(verticalSides);
    }

    public int connectedComponentCount() {
        findConnectedComponents();
        synchronized (lock) {
            return connectedComponents.size();
        }
    }

    public IRectanglesUnion connectedComponent(int index) {
        findConnectedComponents();
        final List<Frame> resultFrames;
        synchronized (lock) {
            resultFrames = connectedComponents.get(index);
        }
        final IRectanglesUnion result = new IRectanglesUnion(resultFrames);
        result.connectedComponents = Collections.singletonList(resultFrames);
        return result;
    }

    public List<HorizontalBoundaryLink> allHorizontalBoundaryLinks() {
        final List<HorizontalBoundaryLink> result = new ArrayList<HorizontalBoundaryLink>();
        for (HorizontalSideSeries series : horizontalSideSeries) {
            result.addAll(series.containedBoundaryLinks);
        }
        return result;
    }

    public List<VerticalBoundaryLink> allVerticalBoundaryLinks() {
        final List<VerticalBoundaryLink> result = new ArrayList<VerticalBoundaryLink>();
        for (VerticalSideSeries series : verticalSidesSeries) {
            result.addAll(series.containedBoundaryLinks);
        }
        return result;
    }

    public void findConnectedComponents() {
        doCreateSideLists();
        synchronized (lock) {
            if (this.connectedComponents != null) {
                return;
            }
        }
        long t1 = System.nanoTime();
        final List<List<Frame>> result = new ArrayList<List<Frame>>();
        if (!frames.isEmpty()) {
            doFindConnectedComponents(result);
        }
        long t2 = System.nanoTime();
        synchronized (lock) {
            this.connectedComponents = result;
        }
        debug(2, "Rectangle union (%d rectangles), finding %d connected components: "
                + "%.3f ms (%.3f mcs / rectangle)%n",
            frames.size(), result.size(),
            (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / (double) frames.size());
    }

    public void findBoundaries() {
        doCreateSideLists();
        synchronized (lock) {
            if (this.allBoundaries != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.allBoundaries = new ArrayList<List<BoundaryLink>>();
                return;
            }
        }
        long t1 = System.nanoTime();
        final List<HorizontalSideSeries> horizontalSeries = createHorizontalSideSeriesLists();
        final List<VerticalSideSeries> verticalSeries = createVerticalSideSeriesLists();
        long t2 = System.nanoTime();
        doFindHorizontalBoundaries(horizontalSeries);
        long t3 = System.nanoTime();
        doConvertHorizontalLinkInfoToAllBoundaryLinks(horizontalSeries, verticalSeries);
        long t4 = System.nanoTime();
        final List<List<BoundaryLink>> result = doJoinBoundaries(horizontalSeries, verticalSeries);
        long t5 = System.nanoTime();
        synchronized (lock) {
            this.horizontalSideSeries = horizontalSeries;
            this.verticalSidesSeries = verticalSeries;
            this.horizontalSidesAtBoundary = null;
            this.verticalSidesAtBoundary = null;
            //TODO!! - actually fill them (or remove them)
            this.allBoundaries = result;
        }
        long t6 = System.nanoTime();
        long totalLinkCount = 0;
        for (List<BoundaryLink> boundary : result) {
            totalLinkCount += boundary.size();
        }
        debug(2, "Rectangle union (%d rectangles), finding %d boundaries with %d links: "
                + "%.3f ms = %.3f initializing + %.3f horizontal links + %.3f vertical links + "
                + "%.3f joining links + %.3f correcting data structures (%.3f mcs / rectangle)%n",
            frames.size(), result.size(), totalLinkCount,
            (t6 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6,
            (t5 - t4) * 1e-6, (t6 - t5) * 1e-6,
            (t6 - t1) * 1e-3 / (double) frames.size());
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "union of " + frames.size() + " rectangles"
                + (connectedComponents == null ? "" : ", " + connectedComponents.size() + " connected components");
        }
    }

    private void doCreateSideLists() {
        synchronized (lock) {
            if (this.horizontalSides != null) {
                return;
            }
        }
        long t1 = System.nanoTime();
        final List<HorizontalSide> horizontalSides = new ArrayList<HorizontalSide>();
        final List<VerticalSide> verticalSides = new ArrayList<VerticalSide>();
        for (Frame frame : frames) {
            horizontalSides.add(frame.lessHorizontalSide);
            horizontalSides.add(frame.higherHorizontalSide);
            verticalSides.add(frame.lessVerticalSide);
            verticalSides.add(frame.higherVerticalSide);
        }
        Collections.sort(horizontalSides);
        Collections.sort(verticalSides);
        long t2 = System.nanoTime();
        synchronized (lock) {
            this.horizontalSides = horizontalSides;
            this.verticalSides = verticalSides;
        }
        debug(2, "Rectangle union (%d rectangles), sorting sides: %.3f ms%n",
            frames.size(), (t2 - t1) * 1e-6);
    }

    private void doFindConnectedComponents(List<List<Frame>> result) {
        final long[] allX = new long[verticalSides.size()];
        assert allX.length > 0;
        // - checked in the calling method
        for (int k = 0; k < allX.length; k++) {
            allX[k] = verticalSides.get(k).boundCoord();
        }
        final boolean[] frameVisited = new boolean[frames.size()];
        final boolean[] added = new boolean[frames.size()];
        // - arrays are filled by false by Java
        final Queue<Frame> queue = new LinkedList<Frame>();
        final List<Frame> neighbours = new ArrayList<Frame>();
        int index = 0;
        for (; ; ) {
            while (index < frameVisited.length && frameVisited[index]) {
                index++;
            }
            if (index >= frameVisited.length) {
                break;
            }
            final List<Frame> component = new ArrayList<Frame>();
            // Breadth-first search:
            queue.add(frames.get(index));
            frameVisited[index] = true;
            while (!queue.isEmpty()) {
                final Frame frame = queue.poll();
                component.add(frame);
                doFindIncidentFrames(neighbours, frame, allX, added);
                for (Frame neighbour : neighbours) {
                    if (!frameVisited[neighbour.index]) {
                        queue.add(neighbour);
                        frameVisited[neighbour.index] = true;
                    }
                }
            }
            result.add(component);
        }
    }

    private void doFindIncidentFrames(List<Frame> result, Frame frame, long[] allX, boolean added[]) {
        result.clear();
        int left = Arrays.binarySearch(allX, frame.fromX);
        assert left >= 0;
        // - we should find at least this frame itself
        assert allX[left] == frame.fromX;
        while (left > 0 && allX[left - 1] == frame.fromX) {
            left--;
        }
        int right = Arrays.binarySearch(allX, frame.toX);
        assert right >= 0;
        // - we should find at least this frame itself
        assert allX[right] == frame.toX;
        while (right + 1 < allX.length && allX[right + 1] == frame.toX) {
            right++;
        }
        for (int k = left; k <= right; k++) {
            final Frame other = verticalSides.get(k).frame;
            assert other.toX >= frame.fromX && other.fromX <= frame.toX : "Binary search in allX failed";
            if (other.toY < frame.fromY || other.fromY > frame.toY) {
                continue;
            }
            if (other == frame) {
                continue;
            }
            // they intersects!
            if (!added[other.index]) {
                result.add(other);
                added[other.index] = true;
                // this flag is necessary to avoid adding twice (for left and right sides)
            }
        }
        for (Frame other : result) {
            added[other.index] = false;
        }
    }

    private List<HorizontalSideSeries> createHorizontalSideSeriesLists() {
        final List<HorizontalSideSeries> result = new ArrayList<HorizontalSideSeries>();
        HorizontalSideSeries last = null;
        for (HorizontalSide side : horizontalSides) {
            boolean expanded = last != null && last.expand(side);
            if (!expanded) {
                result.add(last);
                last = new HorizontalSideSeries(side);
            }
        }
        if (last != null) {
            result.add(last);
        }
        if (DEBUG_LEVEL >= 1) {
            for (int k = 1, n = result.size(); k < n; k++) {
                assert result.get(k - 1).compareTo(result.get(k)) <= 0;
            }
        }
        return result;
    }

    private List<VerticalSideSeries> createVerticalSideSeriesLists() {
        final List<VerticalSideSeries> result = new ArrayList<VerticalSideSeries>();
        VerticalSideSeries last = null;
        for (VerticalSide side : verticalSides) {
            boolean expanded = last != null && last.expand(side);
            if (!expanded) {
                result.add(last);
                last = new VerticalSideSeries(side);
            }
        }
        if (last != null) {
            result.add(last);
        }
        if (DEBUG_LEVEL >= 1) {
            for (int k = 1, n = result.size(); k < n; k++) {
                assert result.get(k - 1).compareTo(result.get(k)) <= 0;
            }
        }
        return result;
    }

    private void doFindHorizontalBoundaries(List<HorizontalSideSeries> horizontalSeries) {
        assert !frames.isEmpty();
        final HorizontalIBracketSet<HorizontalSideSeries> bracketSet =
            new HorizontalIBracketSet<HorizontalSideSeries>(horizontalSeries, true);
        while (bracketSet.next()) {
            final NavigableSet<IBracket> brackets = bracketSet.currentIntersections();
            final IBracket lastBefore = bracketSet.lastIntersectionBeforeLeft();
            boolean lastRightAtBoundary = lastBefore == null || lastBefore.followingCoveringDepth == 0;
            Side lastLeftVertical = lastRightAtBoundary ? bracketSet.horizontal.sideFrom() : null;
            for (IBracket bracket : brackets) {
                assert bracket.covers(bracketSet.y);
                boolean rightAtBoundary = bracket.followingCoveringDepth == 0;
                if (rightAtBoundary == lastRightAtBoundary) {
                    continue;
                }
                if (rightAtBoundary) {
                    lastLeftVertical = bracket.intersectingSide;
                } else {
                    addHorizontalLink(bracketSet, lastLeftVertical, bracket.intersectingSide);
                }
                lastRightAtBoundary = rightAtBoundary;
            }
            if (lastRightAtBoundary) {
                addHorizontalLink(bracketSet, lastLeftVertical, bracketSet.horizontal.sideTo());
            }
        }
    }

    private void doConvertHorizontalLinkInfoToAllBoundaryLinks(
        List<HorizontalSideSeries> horizontalSeries,
        List<VerticalSideSeries> verticalSeries)
    {
        assert !frames.isEmpty();
        /*
        List<List<HorizontalBoundaryLink>> intersectingHorizontals = createListOfLists(verticalSides.size());
        for (List<HorizontalBoundaryLink> linksOnSide : completedContainedBoundaryLinksForHorizontalSides) {
            for (HorizontalBoundaryLink link : linksOnSide) {
                intersectingHorizontals.get(link.transversalSideFrom.indexInSortedList).add(link);
                intersectingHorizontals.get(link.transversalSideTo.indexInSortedList).add(link);
            }
        }
        boolean chainOfVerticalLinks = false;
        VerticalSide lastVerticalSide = null;
        VerticalBoundaryLink lastVerticalLink = null;
        HorizontalBoundaryLink[] horizontalLinks = new HorizontalBoundaryLink[0];
        for (int index = 0, count = intersectingHorizontals.size(); index < count; index++) {
            final VerticalSide verticalSide = verticalSides.get(index);
            final long y = verticalSide.boundCoord();
            final boolean first = verticalSide.first;
            // really we use only information about y and first;
            // several horizontal sides at the same horizontal are processed together
            if (chainOfVerticalLinks) {
                assert lastVerticalSide != null;
                assert lastVerticalLink != null;
                if (!verticalSide.isContinuationOf(lastVerticalSide)) {
                    throw new AssertionError("We met odd number of intersections ane left the vertical!");
                }
                // - it is the only situation, when we can meet odd number of intersections
                // and enter into "chain-of-vertical-links" mode
            }
            final List<HorizontalBoundaryLink> horizontalsList = intersectingHorizontals.get(index);
            final int horizontalsCount = horizontalsList.size();
            if (DEBUG_LEVEL >= 3) {
                System.out.printf("  Vertical #%d, %s; %s%d horizontal sides:%s",
                    index, verticalSide, chainOfVerticalLinks ? "CHAIN; " : "",
                    horizontalsCount, toDebugString(horizontalsList));
            }
            if (horizontalsCount == 0) {
                if (chainOfVerticalLinks) {
                    final VerticalBoundaryLink fullSideLink = new VerticalBoundaryLink(
                        y, first, verticalSide.boundFrom(), verticalSide.boundTo());
                    fullSideLink.linkFrom = lastVerticalLink;
                    lastVerticalLink.linkTo = fullSideLink;
                    resultingContainedBoundaryLinksForVerticalSides.get(index).add(fullSideLink);
                    lastVerticalLink = fullSideLink;
                    lastVerticalSide = verticalSide;
                    continue;
                }
            }
            horizontalLinks = horizontalsList.toArray(horizontalLinks);
            Arrays.sort(horizontalLinks, 0, horizontalsCount);
            int k = 0;
            if (chainOfVerticalLinks) {
                // first horizontal link finishs the chain
                final HorizontalBoundaryLink linkTo = horizontalLinks[0];
                final VerticalBoundaryLink starting = new VerticalBoundaryLink(
                    y, first, verticalSide.boundFrom(), linkTo.coord);
                lastVerticalLink.linkTo = starting;
                k = 1;
                chainOfVerticalLinks = false;
                starting.linkFrom = lastVerticalLink;
                starting.linkTo = linkTo;
                linkTo.setNeighbour(starting);
                resultingContainedBoundaryLinksForVerticalSides.get(index).add(starting);
                lastVerticalLink = starting;
            }
            assert !chainOfVerticalLinks;
            for (; k <= horizontalsCount - 2; k += 2) {
                // even number of horizontal links are simple cases: they do not beglong to any chains
                final HorizontalBoundaryLink linkFrom = horizontalLinks[k];
                final HorizontalBoundaryLink linkTo = horizontalLinks[k + 1];
                final long from = linkFrom.coord;
                final long to = linkTo.coord;
                assert k <= 1 || from > horizontalLinks[k - 1].coord :
                    "Two horizontal links with the same ordinate " + from + "(" + horizontalLinks[k - 1].coord
                        + ") are incident with the same vertical side";
                assert from < to :
                    "Empty vertical link #" + (k / 2) + ": " + from + ".." + to + ", vertical index " + index;
                final VerticalBoundaryLink link = new VerticalBoundaryLink(first, y, linkFrom, linkTo);
                linkFrom.setNeighbour(link);
                linkTo.setNeighbour(link);
                resultingContainedBoundaryLinksForVerticalSides.get(index).add(link);
                lastVerticalLink = link;
            }
            if (k < horizontalsCount) {
                final HorizontalBoundaryLink linkFrom = horizontalLinks[k];
                final VerticalBoundaryLink ending = new VerticalBoundaryLink(
                    y, first, linkFrom.coord, verticalSide.boundTo());
                k++;
                chainOfVerticalLinks = true;
                ending.linkFrom = linkFrom;
                linkFrom.setNeighbour(ending);
                resultingContainedBoundaryLinksForVerticalSides.get(index).add(ending);
                lastVerticalLink = ending;
            }
            assert k == horizontalsCount;
            lastVerticalSide = verticalSide;
        }
        assert !chainOfVerticalLinks;
        */
    }

    private List<List<BoundaryLink>> doJoinBoundaries(
        List<HorizontalSideSeries> horizontalSeries,
        List<VerticalSideSeries> verticalSeries)
    {
        assert !frames.isEmpty();
        //TODO!!
        return new ArrayList<List<BoundaryLink>>();
    }

    static void debug(int level, String format, Object... args) {
        if (DEBUG_LEVEL >= level) {
            System.out.printf(Locale.US, format, args);
        }
    }

    private static void addHorizontalLink(
        HorizontalIBracketSet<HorizontalSideSeries> bracketSet,
        Side firstTransveralSeries,
        Side secondTransveralSeries)
    {
        final HorizontalBoundaryLink link = new HorizontalBoundaryLink(
            bracketSet.horizontal,
            (VerticalSideSeries) firstTransveralSeries,
            (VerticalSideSeries) secondTransveralSeries);
        if (link.from < link.to) {
            if (DEBUG_LEVEL >= 3) {
                System.out.printf("    adding %s%n", link);
            }
            bracketSet.horizontal.containedBoundaryLinks.add(link);
        }
    }

    private static List<Frame> checkAndConvertToFrames(Collection<IRectangularArea> rectangles) {
        if (rectangles == null) {
            throw new NullPointerException("Null rectangles argument");
        }
        for (IRectangularArea rectangle : rectangles) {
            if (rectangle == null) {
                throw new NullPointerException("Null rectangle in a collection");
            }
            if (rectangle.coordCount() != 2) {
                throw new IllegalArgumentException("Only 2-dimensional rectangles can be joined");
            }
        }
        List<Frame> frames = new ArrayList<Frame>();
        int index = 0;
        for (IRectangularArea rectangle : rectangles) {
            frames.add(new Frame(rectangle, index++));
        }
        return frames;
    }

    private static String toDebugString(List<? extends BoundaryLink> links) {
        if (links.isEmpty()) {
            return String.format(" NONE%n");
        }
        StringBuilder sb = new StringBuilder(String.format("%n"));
        for (BoundaryLink link : links) {
            sb.append(String.format("    %s%n", link));
        }
        return sb.toString();
    }


    // The following class is necessary for finding boundary:
    // we must join sides that can be a single boundary link
    // to avoid troubles with links which lie at several frame sides.
    static abstract class SideSeries extends Side {
        final long coord;
        long from;
        long to;
        private FrameSide sideFrom;
        private FrameSide sideTo;

        private SideSeries(FrameSide initialSide) {
            super(initialSide.first);
            this.coord = initialSide.boundCoord();
            this.from = initialSide.boundFrom();
            this.to = initialSide.boundTo();
            this.sideFrom = initialSide.sideFrom();
            this.sideTo = initialSide.sideTo();
            initialSide.containingSeries = this;
        }

        @Override
        public long boundCoord() {
            return coord;
        }

        @Override
        public long boundFrom() {
            return from;
        }

        @Override
        public long boundTo() {
            return to;
        }

        @Override
        SideSeries sideFrom() {
            return sideFrom.containingSeries;
        }

        @Override
        SideSeries sideTo() {
            return sideTo.containingSeries;
        }

        @Override
        long uniqueId() {
            return first ? sideFrom.frame.index : -sideFrom.frame.index - 1;
        }

        boolean expand(FrameSide followingSide) {
            if (followingSide.isHorizontal() != this.isHorizontal() || followingSide.first != this.first) {
                return false;
            }
            final long followingFrom = followingSide.boundFrom();
            final long followingTo = followingSide.boundTo();
            if (followingFrom > to || followingTo < from) {
                return false;
            }
            if (followingFrom < from) {
                from = followingFrom;
                sideFrom = followingSide.sideFrom();
            }
            if (followingTo > to) {
                to = followingTo;
                sideTo = followingSide.sideTo();
            }
            followingSide.containingSeries = this;
            return true;
        }
    }

    static class HorizontalSideSeries extends SideSeries {
        List<HorizontalBoundaryLink> containedBoundaryLinks = new ArrayList<HorizontalBoundaryLink>();
        // Filled after creation, but before publishing references to lists of any series

        public HorizontalSideSeries(FrameSide initialSide) {
            super(initialSide);
        }

        @Override
        public boolean isHorizontal() {
            return true;
        }
    }

    static class VerticalSideSeries extends SideSeries {
        List<VerticalBoundaryLink> containedBoundaryLinks = new ArrayList<VerticalBoundaryLink>();
        // Filled after creation, but before publishing references to lists of any series

        public VerticalSideSeries(FrameSide initialSide) {
            super(initialSide);
        }

        @Override
        public boolean isHorizontal() {
            return false;
        }
    }
}

