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

import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;

import java.util.*;

/**
 * <p>Set-theoretic union of several 2-dimensional {@link IRectangularArea rectangles}
 * with integer integer coordinates of vertices and sides, parallel to coordinate axes.
 * This class allows to solve the following main tasks:
 * <ol>
 * <li>find connected components in this union;</li>
 * <li>find its boundary as a polygon: a sequence of links, where each link is a horizontal or vertical
 * segment (1st link is horizontal, 2nd is vertical, 3rd is horizontal, etc.);</li>
 * <li>find the largest rectangle (with sides, parallel to the coordinate axes), which is a subset of this union.</li>
 * </ol>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.
 * However, all important information is returned "lazily", i.e. while the 1st attempt to read it.
 * So, the instance creation method {@link #newInstance(Collection)}
 * works quickly and does not lead to complex calculations and allocating additional memory.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class IRectanglesUnion {
    static final int DEBUG_LEVEL = net.algart.arrays.Arrays.SystemSettings.getIntProperty(
        "net.algart.math.rectangles.debugLevel", 0);

    private static final boolean USE_SECOND_SIDES_WHILE_SEARCHING_CONNECTIONS = false;
    // - it seems that we can prove that this can be false always

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

        @Override
        public String toString() {
            return "Frame #" + index + " (" + rectangle + ")";
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
            return isFirstOfTwoParallelSides() ? coord() : coord() - 1;
        }

        /**
         * Returns the coordinate of this frame side along the coordinate axis,
         * to which this side is perpendicular, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the perpendicular coordinate of this side + 0.5
         */
        public abstract long coord();

        /**
         * Returns the starting coordinate of this frame side along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the starting coordinate of this side + 0.5
         */
        public abstract long from();

        /**
         * Returns the ending coordinate of this frame side along the coordinate axis,
         * to which this link is parallel, increased by 0.5
         * (the sides always have half-integer coordinates).
         *
         * @return the ending coordinate of this side + 0.5
         */
        public abstract long to();

        public IRectangularArea equivalentRectangle() {
            final long coord = frameSideCoord();
            final long from = from();
            final long to = to();
            assert from <= to;
            if (from == to) {
                return null;
            } else if (isHorizontal()) {
                return IRectangularArea.valueOf(from, coord, to - 1, coord);
            } else {
                return IRectangularArea.valueOf(coord, from, coord, to - 1);
            }
        }

        @Override
        public int compareTo(Side o) {
            if (this.getClass() != o.getClass()) {
                throw new ClassCastException("Comparison of sides with different types: "
                    + getClass() + " != " + o.getClass());
            }
            final long thisCoord = this.coord();
            final long otherCoord = o.coord();
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
            if (this.first && !o.first) {
                return -1;
            }
            if (!this.first && o.first) {
                return 1;
            }
            // Sorting along another coordinate is necessary for searching several sides,
            // which are really a single continuous segment
            final long thisFrom = this.from();
            final long otherFrom = o.from();
            if (thisFrom < otherFrom) {
                return -1;
            }
            if (thisFrom > otherFrom) {
                return 1;
            }
            final long thisTo = this.to();
            final long otherTo = o.to();
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
                + ": " + from() + ".." + to() + " at " + coord();
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
            return this.coord() == side.coord()
                && this.from() == side.from()
                && this.to() == side.to();
        }

        @Override
        public int hashCode() {
            int result = (first ? 1 : 0);
            final long coord = coord();
            final long from = from();
            final long to = to();
            result = 31 * result + (int) (coord ^ (coord >>> 32));
            result = 31 * result + (int) (from ^ (from >>> 32));
            result = 31 * result + (int) (to ^ (to >>> 32));
            return result;
        }

        abstract void allContainedFrameSides(List<FrameSide> result);

        abstract FrameSide transversalFrameSideFrom();

        abstract FrameSide transversalFrameSideTo();
    }

    public static abstract class FrameSide extends Side {
        final Frame frame;
        SideSeries parentSeries = null;

        private FrameSide(boolean first, Frame frame) {
            super(first);
            assert frame != null;
            this.frame = frame;
        }

        public Frame frame() {
            return frame;
        }


        @Override
        void allContainedFrameSides(List<FrameSide> result) {
            result.clear();
            result.add(this);
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
        public long coord() {
            return first ? frame.fromY : frame.toY;
        }

        @Override
        public long from() {
            return frame.fromX;
        }

        @Override
        public long to() {
            return frame.toX;
        }

        @Override
        FrameSide transversalFrameSideFrom() {
            return frame.lessVerticalSide;
        }

        @Override
        FrameSide transversalFrameSideTo() {
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
        public long coord() {
            return first ? frame.fromX : frame.toX;
        }

        @Override
        public long from() {
            return frame.fromY;
        }

        @Override
        public long to() {
            return frame.toY;
        }

        @Override
        FrameSide transversalFrameSideFrom() {
            return frame.lessVerticalSide;
        }

        @Override
        FrameSide transversalFrameSideTo() {
            return frame.higherVerticalSide;
        }
    }

    public static abstract class BoundaryLink implements Comparable<BoundaryLink> {
        final SideSeries parentSeries;
        long from;
        long to;
        int indexInSortedList = -1;
        boolean joinedIntoAllBoundaries = false;

        private BoundaryLink(
            SideSeries parentSeries,
            long from,
            long to)
        {
            assert parentSeries != null;
            this.parentSeries = parentSeries;
            assert from <= to;
            this.from = from;
            this.to = to;
        }

        public boolean atFirstOfTwoParallelSides() {
            return parentSeries.first;
        }

        public boolean atSecondOfTwoParallelSides() {
            return !parentSeries.first;
        }

        public abstract boolean isHorizontal();

        /**
         * Returns the coordinate of this boundary element (link) along the coordinate axis,
         * to which this link is perpendicular, increased by 0.5
         * (the bounrady always has half-integer coordinate).
         *
         * @return the perpendicular coordinate of this link + 0.5
         */
        public long coord() {
            return parentSeries.coord();
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

        public abstract BoundaryLink linkFrom();

        public abstract BoundaryLink linkTo();

        public abstract IRectangularArea equivalentRectangle();

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
            if (this.to < o.to) {
                return -1;
            }
            if (this.to > o.to) {
                return 1;
            }
            return this.parentSeries.compareTo(o.parentSeries);
        }

        @Override
        public String toString() {
            return (this instanceof HorizontalBoundaryLink ? "horizontal" : "vertical")
                + " boundary link " + from + ".." + to + " at " + parentSeries;
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
            return this.parentSeries.equals(that.parentSeries);
        }

        @Override
        public int hashCode() {
            int result = parentSeries.hashCode();
            result = 31 * result + (int) (from ^ (from >>> 32));
            result = 31 * result + (int) (to ^ (to >>> 32));
            return result;
        }

        double areaUnderLink() {
            final double area = ((double) to - (double) from) * (double) coord();
            return atFirstOfTwoParallelSides() ? -area : area;
        }
    }

    public static class HorizontalBoundaryLink extends BoundaryLink {
        VerticalSideSeries transversalSeriesFrom;
        VerticalSideSeries transversalSeriesTo;
        // - these two fields are necessary only while constructing the boundary
        VerticalBoundaryLink linkFrom = null;
        VerticalBoundaryLink linkTo = null;

        private HorizontalBoundaryLink(
            HorizontalSideSeries parentSeries,
            VerticalSideSeries transversalSeriesFrom,
            VerticalSideSeries transversalSeriesTo)
        {
            super(parentSeries,
                transversalSeriesFrom.coord(),
                transversalSeriesTo.coord());
            this.transversalSeriesFrom = transversalSeriesFrom;
            this.transversalSeriesTo = transversalSeriesTo;
        }

        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public VerticalBoundaryLink linkFrom() {
            return linkFrom;
        }

        @Override
        public VerticalBoundaryLink linkTo() {
            return linkTo;
        }

        @Override
        public IRectangularArea equivalentRectangle() {
            return IRectangularArea.valueOf(
                from, parentSeries.frameSideCoord(),
                to - 1, parentSeries.frameSideCoord());
        }

        void setNeighbour(VerticalBoundaryLink neighbour) {
            if (neighbour.parentSeries == transversalSeriesFrom) {
                linkFrom = neighbour;
            } else if (neighbour.parentSeries == transversalSeriesTo) {
                linkTo = neighbour;
            } else {
                throw new AssertionError("Attempt to assing vertical neighbour from alien side series");
            }
        }
    }

    public static class VerticalBoundaryLink extends BoundaryLink {
        final HorizontalBoundaryLink linkFrom;
        final HorizontalBoundaryLink linkTo;

        private VerticalBoundaryLink(
            VerticalSideSeries parentSeries,
            HorizontalBoundaryLink linkFrom,
            HorizontalBoundaryLink linkTo)
        {
            super(parentSeries, linkFrom.coord(), linkTo.coord());
            this.linkFrom = linkFrom;
            this.linkTo = linkTo;
        }

        @Override
        public boolean isHorizontal() {
            return false;
        }

        @Override
        public HorizontalBoundaryLink linkFrom() {
            return linkFrom;
        }

        @Override
        public HorizontalBoundaryLink linkTo() {
            return linkTo;
        }

        @Override
        public IRectangularArea equivalentRectangle() {
            return IRectangularArea.valueOf(
                parentSeries.frameSideCoord(), from,
                parentSeries.frameSideCoord(), to - 1);
        }
    }


    private final List<Frame> frames;
    private final IRectangularArea circumscribedRectangle;

    private List<HorizontalSide> horizontalSides = null;
    private List<VerticalSide> verticalSides = null;
    private List<List<Frame>> connectedComponents = null;
    private List<HorizontalSideSeries> horizontalSideSeries = null;
    private List<VerticalSideSeries> verticalSideSeries = null;
    private List<HorizontalSideSeries> horizontalSideSeriesAtBoundary = null;
    private List<VerticalSideSeries> verticalSideSeriesAtBoundary = null;
    private long[] allDifferentXAtBoundary = null;
    private double unionArea = Double.NaN;
    private List<List<BoundaryLink>> allBoundaries = null;
    private List<HorizontalSection> horizontalSectionsByLowerSides = null;
    // - this field is accessed via reflection in the test
    private IRectangularArea largestRectangleInUnion = null;
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

    /**
     * Subtracts the given rectangle (via {@link IRectangularArea#subtractCollection(Queue, Collection)} method)
     * from the set of rectangles, containing in this object, and return the union of the resulting rectangles.
     *
     * @param whatToSubtract the subtracted rectangle.
     * @return the union of the rest of rectangles after subtraction.
     * @throws NullPointerException     if the argument is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>whatToSubtract.coordCount() != 2</tt>.
     */
    public IRectanglesUnion subtractRectangle(IRectangularArea whatToSubtract) {
        if (whatToSubtract == null) {
            throw new NullPointerException("Null rectangle");
        }
        if (whatToSubtract.coordCount() != 2) {
            throw new IllegalArgumentException("Only 2-dimensional rectangle can be subtracted");
        }
        Queue<IRectangularArea> rectangles = new LinkedList<IRectangularArea>();
        for (Frame frame : frames) {
            rectangles.add(frame.rectangle);
        }
        IRectangularArea.subtractCollection(rectangles, whatToSubtract);
        return newInstance(rectangles);
    }

    public IRectanglesUnion subtractLargestRectangle() {
        if (frames.isEmpty()) {
            return this;
        }
        return subtractRectangle(largestRectangleInUnion());
    }

    public List<Frame> frames() {
        return Collections.unmodifiableList(frames);
    }

    public IRectangularArea circumscribedRectangle() {
        return circumscribedRectangle;
    }

    public List<HorizontalSide> horizontalSides() {
        synchronized (lock) {
            return Collections.unmodifiableList(horizontalSides);
        }
    }

    public List<VerticalSide> verticalSides() {
        synchronized (lock) {
            return Collections.unmodifiableList(verticalSides);
        }
    }

    public int connectedComponentCount() {
        synchronized (lock) {
            findConnectedComponents();
            return connectedComponents.size();
        }
    }

    public IRectanglesUnion connectedComponent(int index) {
        synchronized (lock) {
            findConnectedComponents();
            final List<Frame> resultFrames = cloneFrames(connectedComponents.get(index));
            final IRectanglesUnion result = new IRectanglesUnion(resultFrames);
            result.connectedComponents = Collections.singletonList(resultFrames);
            return result;
        }
    }

    public List<HorizontalBoundaryLink> allHorizontalBoundaryLinks() {
        synchronized (lock) {
            findBoundaries();
            final List<HorizontalBoundaryLink> result = new ArrayList<HorizontalBoundaryLink>();
            for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
                result.addAll(series.containedBoundaryLinks);
            }
            return result;
        }
    }

    public List<VerticalBoundaryLink> allVerticalBoundaryLinks() {
        synchronized (lock) {
            findBoundaries();
            final List<VerticalBoundaryLink> result = new ArrayList<VerticalBoundaryLink>();
            for (VerticalSideSeries series : verticalSideSeriesAtBoundary) {
                result.addAll(series.containedBoundaryLinks);
            }
            return result;
        }
    }

    // First boundary in the result is the external contour.
    public List<List<BoundaryLink>> allBoundaries() {
        synchronized (lock) {
            findBoundaries();
            return Collections.unmodifiableList(allBoundaries);
        }
    }

    public double unionArea() {
        synchronized (lock) {
            findBoundaries();
            return unionArea;
        }
    }

    public IRectangularArea largestRectangleInUnion() {
        synchronized (lock) {
            findLargestRectangleInUnion();
            return largestRectangleInUnion;
        }
    }

    /**
     * Forces this object to find all connected components.
     * It does not affect to results of any other methods, but after this call the following methods
     * will work quickly:
     * <ul>
     * <li>{@link #connectedComponentCount()}</li>
     * <li>{@link #connectedComponent(int)}</li>
     * <li>{@link #horizontalSides()}</li>
     * <li>{@link #verticalSides()}</li>
     * </ul>
     */
    public void findConnectedComponents() {
        synchronized (lock) {
            doCreateSideLists();
            if (this.connectedComponents != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.connectedComponents = Collections.emptyList();
                return;
            }
            long t1 = System.nanoTime();
            final List<List<Frame>> connectionLists = createListOfLists(frames.size());
            long t2 = System.nanoTime();
            final long nConnections = doFillConnectionLists(connectionLists);
            long t3 = System.nanoTime();
            final List<List<Frame>> result = doFindConnectedComponents(connectionLists);
            long t4 = System.nanoTime();
            this.connectedComponents = result;
            debug(1, "Rectangle union (%d rectangles), finding %d connected components: "
                    + "%.3f ms = %.3f ms initializing + %.3f finding %d connections + %.3f breadth-first search "
                    + "(%.3f mcs / rectangle)%n",
                frames.size(), result.size(),
                (t4 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, nConnections, (t4 - t3) * 1e-6,
                (t4 - t1) * 1e-3 / (double) frames.size());
            if (DEBUG_LEVEL >= 2 && result.size() >= 2) {
                t1 = System.nanoTime();
                for (int i = 0; i < result.size(); i++) {
                    for (Frame frame1 : result.get(i)) {
                        for (int j = i + 1; j < result.size(); j++) {
                            for (Frame frame2 : result.get(j)) {
                                if (frame1.rectangle.intersects(frame2.rectangle)) {
                                    // Note: this check is even more strong than requirement of this class
                                    // (attached rectangles are considered to be in the single component)
                                    throw new AssertionError("First 2 connected component really have intersection: "
                                        + frame1 + " (component " + i + ") intersects "
                                        + frame2 + " (component " + j + ")");
                                }
                            }
                        }
                    }
                }
                t2 = System.nanoTime();
                debug(2, "Testing connected components: %.3f ms%n", (t2 - t1) * 1e-6);
            }
        }
    }

    /**
     * Forces this object to find the boundary of this union of rectangles.
     * It does not affect to results of any other methods, but after this call the following methods
     * will work quickly:
     * <ul>
     * <li>{@link #allHorizontalBoundaryLinks()}</li>
     * <li>{@link #allVerticalBoundaryLinks()}</li>
     * <li>{@link #allBoundaries()}</li>
     * <li>{@link #horizontalSides()}</li>
     * <li>{@link #verticalSides()}</li>
     * </ul>
     */
    public void findBoundaries() {
        synchronized (lock) {
            doCreateSideLists();
            // Global synchronization necessary, because we change internal fields like parentSeries
            // in already existing structures.
            if (this.allBoundaries != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.horizontalSideSeries = Collections.emptyList();
                this.verticalSideSeries = Collections.emptyList();
                this.horizontalSideSeriesAtBoundary = Collections.emptyList();
                this.verticalSideSeriesAtBoundary = Collections.emptyList();
                this.allDifferentXAtBoundary = new long[0];
                this.unionArea = 0.0;
                this.allBoundaries = Collections.emptyList();
                return;
            }
            long t1 = System.nanoTime();
            this.horizontalSideSeries = createHorizontalSideSeriesLists();
            this.verticalSideSeries = createVerticalSideSeriesLists();
            long t2 = System.nanoTime();
            final long hCount = doFindHorizontalBoundaries();
            long t3 = System.nanoTime();
            this.horizontalSideSeriesAtBoundary = new ArrayList<HorizontalSideSeries>();
            doExtractHorizontalSeriesAtBoundary();
            long t4 = System.nanoTime();
            final long vCount = doConvertHorizontalToVerticalLinks();
            this.verticalSideSeriesAtBoundary = new ArrayList<VerticalSideSeries>();
            long t5 = System.nanoTime();
            doExtractVerticalSeriesAtBoundary();
            if (vCount != hCount) {
                throw new AssertionError("Different numbers of horizontal and vertical links found");
            }
            long t6 = System.nanoTime();
            this.allDifferentXAtBoundary = doExtractAllDifferentXAtBoundary();
            this.unionArea = doCalculateArea();
            doSetLinkIndexes(hCount);
            assert hCount <= Integer.MAX_VALUE;
            // - it was checked in doSetLinkIndexes()
            this.allBoundaries = doJoinBoundaries(hCount);
            long t7 = System.nanoTime();
            if (DEBUG_LEVEL >= 1) {
                long totalLinkCount = totalCount(allBoundaries);
                debug(1, "Rectangle union (%d rectangles), area %.1f, finding %d boundaries with %d links: "
                        + "%.3f ms = %.3f ms initializing "
                        + "+ %.3f ms %d horizontal links "
                        + "+ %.3f ms %d/%d horizontals at boundary "
                        + "+ %.3f ms %d vertical links "
                        + "+ %.3f ms %d/%d verticals at boundary "
                        + "+ %.3f ms postprocessing and joining links "
                        + "(%.3f mcs / rectangle, %.3f mcs / link)%n",
                    frames.size(), unionArea, allBoundaries().size(), totalLinkCount,
                    (t7 - t1) * 1e-6, (t2 - t1) * 1e-6,
                    (t3 - t2) * 1e-6, hCount,
                    (t4 - t3) * 1e-6, horizontalSideSeriesAtBoundary.size(), horizontalSideSeries.size(),
                    (t5 - t4) * 1e-6, vCount,
                    (t6 - t5) * 1e-6, verticalSideSeriesAtBoundary.size(), verticalSideSeries.size(),
                    (t7 - t6) * 1e-6,
                    (t7 - t1) * 1e-3 / (double) frames.size(), (t7 - t1) * 1e-3 / (double) totalLinkCount);
            }
            if (DEBUG_LEVEL >= 2) {
                final StringBuilder sb = new StringBuilder();
                for (int k = 0; k < allDifferentXAtBoundary.length; k++) {
                    if (k == 100) {
                        sb.append("...");
                        break;
                    }
                    sb.append("(").append(k).append(":)").append(allDifferentXAtBoundary[k]).append(", ");
                }
                debug(2, "Found %d verticals with different x: %s%n", allDifferentXAtBoundary.length, sb);
            }
        }
    }

    /**
     * Forces this object to find the largest rectangle (with sides, parallel to the coordinate axes),
     * which is a subset of this union of rectangles.
     * It does not affect to results of any other methods, but after this call the following methods
     * will work quickly:
     * <ul>
     * <li>{@link #largestRectangleInUnion()}</li>
     * <li>{@link #allHorizontalBoundaryLinks()}</li>
     * <li>{@link #allVerticalBoundaryLinks()}</li>
     * <li>{@link #allBoundaries()}</li>
     * <li>{@link #horizontalSides()}</li>
     * <li>{@link #verticalSides()}</li>
     * </ul>
     */
    public void findLargestRectangleInUnion() {
        synchronized (lock) {
            findBoundaries();
            if (this.largestRectangleInUnion != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.horizontalSectionsByLowerSides = Collections.emptyList();
                this.largestRectangleInUnion = null;
                return;
            }
            long t1 = System.nanoTime();
            final List<HorizontalSection> horizontalSectionsByLowerSides = doFindHorizontalSections();
            long t2 = System.nanoTime();
            final List<List<HorizontalBoundaryLink>> closingLinksIntersectingEachVertical =
                doFindClosingLinksIntersectingEachVertical();
            long t3 = System.nanoTime();
            final SearchIRectangleInHypograph searcher = doFindLargestRegtangles(
                horizontalSectionsByLowerSides,
                closingLinksIntersectingEachVertical);
            long t4 = System.nanoTime();
            this.horizontalSectionsByLowerSides = horizontalSectionsByLowerSides;
            this.largestRectangleInUnion = searcher.largestRectangle();
            if (DEBUG_LEVEL >= 1) {
                long totalLinkCount = totalCount(allBoundaries);
                debug(1, "Rectangle union (%d rectangles, %d links), "
                        + "finding largest rectangle %s (area %.1f): "
                        + "%.3f ms = %.3f ms %d horizontal sections "
                        + "+ %.3f ms %d links "
                        + "intersecting with %d verticals "
                        + "+ %.3f ms searching largest rectangle "
                        + "(%.3f mcs / rectangle, %.3f mcs / link)%n",
                    frames.size(), totalLinkCount,
                    largestRectangleInUnion, largestRectangleInUnion.volume(),
                    (t4 - t1) * 1e-6, (t2 - t1) * 1e-6, horizontalSectionsByLowerSides.size(),
                    (t3 - t2) * 1e-6, totalCount(closingLinksIntersectingEachVertical),
                    allDifferentXAtBoundary.length - 1,
                    (t4 - t3) * 1e-6,
                    (t4 - t1) * 1e-3 / (double) frames.size(), (t4 - t1) * 1e-3 / (double) totalLinkCount);
            }
            if (DEBUG_LEVEL >= 2) {
                t1 = System.nanoTime();
                for (HorizontalSection section : horizontalSectionsByLowerSides) {
                    IRectangularArea r = section.equivalentRectangle();
                    Queue<IRectangularArea> queue = new LinkedList<IRectangularArea>();
                    queue.add(r);
                    for (Frame frame : frames) {
                        IRectangularArea.subtractCollection(queue,
                            IRectangularArea.valueOf(frame.fromX, frame.fromY, frame.toX - 1, frame.toY));
                        // Note: we need to use toY instead of toY-1, because the section lies BETWEEN pixels
                    }
                    if (!queue.isEmpty()) {
                        throw new AssertionError("Section " + section + " is not a subset of the union");
                    }
                }
                t2 = System.nanoTime();
                debug(2, "Testing horizontal sections: %.3f ms%n", (t2 - t1) * 1e-6);
            }
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return "union of " + frames.size() + " rectangles"
                + (connectedComponents == null ? "" : ", " + connectedComponents.size() + " connected components");
        }
    }

    public static double areaInBoundary(List<BoundaryLink> boundary) {
        if (boundary == null) {
            throw new NullPointerException("Null boundary");
        }
        double result = 0.0;
        for (BoundaryLink link : boundary) {
            if (link.isHorizontal()) {
                result += link.areaUnderLink();
            }
        }
        return result;
    }

    public static List<IPoint> boundaryVerticesPlusHalf(List<BoundaryLink> boundary) {
        if (boundary == null) {
            throw new NullPointerException("Null boundary");
        }
        final List<IPoint> result = new ArrayList<IPoint>();
        BoundaryLink last = null;
        for (BoundaryLink link : boundary) {
            final long coord = link.coord();
            final long secondCoord = last == link.linkTo() ? link.to : link.from;
            // Note: in boundaries, built by this class, last.isHorizontal() != link.isHorizontal(),
            // but this may be not so in third-party boundaries.
            result.add(link.isHorizontal() ?
                IPoint.valueOf(secondCoord, coord) :
                IPoint.valueOf(coord, secondCoord));
            last = link;
        }
        if (DEBUG_LEVEL >= 3) {
            debug(3, "Boundary precise vertices +0.5: %s%n", result);
        }
        return result;
    }

    public static List<IPoint> boundaryVerticesAtRectangles(List<BoundaryLink> boundary) {
        if (boundary == null) {
            throw new NullPointerException("Null boundary");
        }
        final List<IPoint> result = new ArrayList<IPoint>();
        final int n = boundary.size();
        if (n == 0) {
            return result;
        }
        BoundaryLink last = boundary.get(n - 1);
        for (BoundaryLink link : boundary) {
            final boolean lastFirst = last.atFirstOfTwoParallelSides();
            final boolean thisFirst = link.atFirstOfTwoParallelSides();
            // Note: in boundaries, built by this class, last.isHorizontal() != link.isHorizontal(),
            // but this may be not so in third-party boundaries.
            final long coord = thisFirst ? link.coord() : link.coord() - 1;
            final long secondCoord =
                last == link.linkTo()
                    ? lastFirst ? link.to : link.to - 1
                    : lastFirst ? link.from : link.from - 1;
            result.add(link.isHorizontal() ?
                IPoint.valueOf(secondCoord, coord) :
                IPoint.valueOf(coord, secondCoord));
            last = link;
        }
        if (DEBUG_LEVEL >= 1) {
            int k = 0;
            for (BoundaryLink link : boundary) {
                final IPoint v1 = result.get(k);
                final IPoint v2 = result.get(k == n - 1 ? 0 : k + 1);
                final IRectangularArea onVertices = IRectangularArea.valueOf(v1.min(v2), v1.max(v2));
                final IRectangularArea onLink = link.equivalentRectangle();
                if (!onVertices.contains(onLink)) {
                    throw new AssertionError("Boundary rectangle does not contain the link #"
                        + k + ": " + v1 + ", " + v2 + ", " + onLink);
                }
                if (onVertices.volume() - onLink.volume() > 2.0001) {
                    throw new AssertionError("Boundary rectangle is too large: link #"
                        + k + ": " + v1 + ", " + v2 + ", " + onLink);
                }
                k++;
            }
        }
        if (DEBUG_LEVEL >= 3) {
            debug(3, "Boundary vertices at rectangles: %s%n", result);
        }
        return result;
    }

    private void doCreateSideLists() {
        if (this.horizontalSides != null) {
            return;
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
        this.horizontalSides = horizontalSides;
        this.verticalSides = verticalSides;
        debug(1, "Rectangle union (%d rectangles), allocating and sorting sides: %.3f ms%n",
            frames.size(), (t2 - t1) * 1e-6);
    }

    private long doFillConnectionLists(List<List<Frame>> connectionLists) {
        assert !frames.isEmpty();
        final HorizontalIBracketSet<HorizontalSide> bracketSet =
            new HorizontalIBracketSet<HorizontalSide>(horizontalSides, false);
        long count = 0;
        while (bracketSet.next()) {
            if (bracketSet.horizontal.first) {
                final IBracket lastBefore = bracketSet.lastIntersectionBeforeLeft();
                if (lastBefore != null && lastBefore.followingCoveringDepth > 0) {
                    // Special case. We can be sure that this previous frame is in the same connected component,
                    // and we need to add a fictive connection with it to correctly process situation,
                    // when some frame or set of frames lies strictly inside another rectangle.
                    // Moreover, this fictive link allows to check only first horizontal sides:
                    // even it fully lies inside another rectangle and does not intersect its vertical sides,
                    // we can reach that rectangle via such fictive limks.
                    addConnection(connectionLists, lastBefore.intersectingSide.frame, bracketSet.horizontal.frame);
                    count++;
                }
            }
            if (USE_SECOND_SIDES_WHILE_SEARCHING_CONNECTIONS || bracketSet.horizontal.first) {
                for (IBracket bracket : bracketSet.currentIntersections()) {
                    if (DEBUG_LEVEL >= 2) {
                        assert bracket.covers(bracketSet.coord);
                    }
                    addConnection(connectionLists, bracket.intersectingSide.frame, bracketSet.horizontal.frame);
                    count++;
                }
            }
        }
        return count;
    }

    private List<List<Frame>> doFindConnectedComponents(List<List<Frame>> connectionLists) {
        assert !frames.isEmpty();
        final List<List<Frame>> result = new ArrayList<List<Frame>>();
        final boolean[] frameVisited = new boolean[frames.size()];
        // - filled by false by Java
        final Queue<Frame> queue = new LinkedList<Frame>();
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
                final List<Frame> neighbours = connectionLists.get(frame.index);
                for (Frame neighbour : neighbours) {
                    if (!frameVisited[neighbour.index]) {
                        queue.add(neighbour);
                        frameVisited[neighbour.index] = true;
                    }
                }
                if (DEBUG_LEVEL >= 4) {
                    debug(4, "  Neighbours of %s:%n", frame);
                    for (Frame neighbour : neighbours) {
                        debug(4, "    %s%n", neighbour);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    private List<HorizontalSideSeries> createHorizontalSideSeriesLists() {
        final List<HorizontalSideSeries> result = new ArrayList<HorizontalSideSeries>();
        HorizontalSideSeries last = null;
        for (HorizontalSide side : horizontalSides) {
            boolean expanded = last != null && last.expand(side);
            if (!expanded) {
                if (last != null) {
                    result.add(last);
                }
                last = new HorizontalSideSeries(side);
            }
        }
        if (last != null) {
            result.add(last);
        }
        checkSidesSeriesList(result);
        return result;
    }

    private List<VerticalSideSeries> createVerticalSideSeriesLists() {
        final List<VerticalSideSeries> result = new ArrayList<VerticalSideSeries>();
        VerticalSideSeries last = null;
        for (VerticalSide side : verticalSides) {
            boolean expanded = last != null && last.expand(side);
            if (!expanded) {
                if (last != null) {
                    result.add(last);
                }
                last = new VerticalSideSeries(side);
            }
        }
        if (last != null) {
            result.add(last);
        }
        checkSidesSeriesList(result);
        return result;
    }

    private void checkSidesSeriesList(List<? extends SideSeries> sideSeries) {
        if (DEBUG_LEVEL >= 2) {
            for (int k = 1, n = sideSeries.size(); k < n; k++) {
                assert sideSeries.get(k - 1).compareTo(sideSeries.get(k)) <= 0;
            }
        }
        if (DEBUG_LEVEL >= 3) {
            debug(3, "  %d side series:%n", sideSeries.size());
            for (int k = 0, n = sideSeries.size(); k < n; k++) {
                debug(3, "    side series %d/%d: %s%n", k, n, sideSeries.get(k));
            }
        }
    }

    private long doFindHorizontalBoundaries() {
        assert !frames.isEmpty();
        final HorizontalIBracketSet<HorizontalSideSeries> bracketSet =
            new HorizontalIBracketSet<HorizontalSideSeries>(horizontalSideSeries, true);
        long count = 0;
        while (bracketSet.next()) {
            final Set<IBracket> brackets = bracketSet.currentIntersections();
            final IBracket lastBefore = bracketSet.lastIntersectionBeforeLeft();
            boolean lastRightAtBoundary = lastBefore == null || lastBefore.followingCoveringDepth == 0;
            FrameSide lastLeftVertical = lastRightAtBoundary ? bracketSet.horizontal.transversalFrameSideFrom() : null;
            for (IBracket bracket : brackets) {
                if (DEBUG_LEVEL >= 2) {
                    assert bracket.covers(bracketSet.coord);
                }
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
                addHorizontalLink(bracketSet, lastLeftVertical, bracketSet.horizontal.transversalFrameSideTo());
            }
            count += bracketSet.horizontal.containedLinksCount();
        }
        return count;
    }

    private long doConvertHorizontalToVerticalLinks() {
        assert !frames.isEmpty();
        for (VerticalSideSeries verticalSeries : verticalSideSeries) {
            assert verticalSeries.containedBoundaryLinks == null :
                "non-null containedBoundaryLinks = " + verticalSeries.containedBoundaryLinks;
            assert verticalSeries.intersectingBoundaryLinks == null :
                "non-null intersectingBoundaryLinks = " + verticalSeries.intersectingBoundaryLinks;
        }
        for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                link.transversalSeriesFrom.addIntersectingLink(link);
                link.transversalSeriesTo.addIntersectingLink(link);
            }
        }
        HorizontalBoundaryLink[] horizontalLinks = new HorizontalBoundaryLink[0];
        long count = 0;
        for (VerticalSideSeries verticalSeries : verticalSideSeries) {
            if (verticalSeries.intersectingBoundaryLinks == null) {
                // - no links added here by the previous loop
                continue;
            }
            final int horizontalsCount = verticalSeries.intersectingBoundaryLinks.size();
            assert horizontalsCount > 0 && horizontalsCount % 2 == 0 : "Invalid horizontalsCount=" + horizontalsCount;
            horizontalLinks = verticalSeries.intersectingBoundaryLinks.toArray(horizontalLinks);
            Arrays.sort(horizontalLinks, 0, horizontalsCount);
            for (int k = 0; k < horizontalsCount; k += 2) {
                final HorizontalBoundaryLink linkFrom = horizontalLinks[k];
                final HorizontalBoundaryLink linkTo = horizontalLinks[k + 1];
                final long from = linkFrom.coord();
                final long to = linkTo.coord();
                assert k == 0 || from > horizontalLinks[k - 1].coord() :
                    "Two horizontal links with the same ordinate " + from + " (=" + horizontalLinks[k - 1].coord()
                        + ") are incident with the same vertical side";
                assert from < to :
                    "Empty vertical link #" + (k / 2) + ": " + from + ".." + to;
                final VerticalBoundaryLink link = new VerticalBoundaryLink(verticalSeries, linkFrom, linkTo);
                linkFrom.setNeighbour(link);
                linkTo.setNeighbour(link);
                verticalSeries.addLink(link);
            }
            count += verticalSeries.containedLinksCount();
        }
        return count;
    }

    private void doExtractHorizontalSeriesAtBoundary() {
        int count = 0;
        for (HorizontalSideSeries series : horizontalSideSeries) {
            if (series.containsBoundary()) {
                series.indexInSortedListAtBoundary = count++;
                this.horizontalSideSeriesAtBoundary.add(series);
            }
        }
    }

    private void doExtractVerticalSeriesAtBoundary() {
        int count = 0;
        for (VerticalSideSeries series : verticalSideSeries) {
            if (series.containsBoundary()) {
                series.indexInSortedListAtBoundary = count++;
                this.verticalSideSeriesAtBoundary.add(series);
            }
        }
    }

    private long[] doExtractAllDifferentXAtBoundary() {
        int count = 0;
        long lastX = 157;
        for (VerticalSideSeries series : verticalSideSeriesAtBoundary) {
            assert series.containsBoundary();
            final long coord = series.coord();
            if (count == 0 || coord != lastX) {
                lastX = coord;
                count++;
            }
            series.numberOfLessCoordinatesAtBoundary = count - 1;
        }
        final long[] result = new long[count];
        count = 0;
        for (VerticalSideSeries series : verticalSideSeriesAtBoundary) {
            final long coord = series.coord();
            if (count == 0 || coord != lastX) {
                lastX = coord;
                result[count] = coord;
                count++;
            }
        }
        return result;
    }

    private void doSetLinkIndexes(long horizontalOrVerticalLinksCount) {
        int count = 0;
        for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                if (count == Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("Number of horizontal links must be < 2^31");
                }
                link.indexInSortedList = count++;
            }
        }
        assert count == horizontalOrVerticalLinksCount;
        count = 0;
        for (VerticalSideSeries series : verticalSideSeriesAtBoundary) {
            for (VerticalBoundaryLink link : series.containedBoundaryLinks) {
                if (count == Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("Number of vertical links must be < 2^31");
                }
                link.indexInSortedList = count++;
            }
        }
        assert count == horizontalOrVerticalLinksCount;
    }

    private double doCalculateArea() {
        double result = 0.0;
        for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                result += link.areaUnderLink();
            }
        }
        return result;
    }

    private List<List<BoundaryLink>> doJoinBoundaries(long numberOfHorizontalLinks) {
        assert !frames.isEmpty();
        final long maxCount = 10 * Math.min(numberOfHorizontalLinks, Integer.MAX_VALUE);
        // really the limit is 2 * numberOfHorizontalLinks;
        // Integer.MAX_VALUE is also the limit due to 31-bit result.size()
        List<List<BoundaryLink>> result = new ArrayList<List<BoundaryLink>>();
        for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                if (!link.joinedIntoAllBoundaries) {
                    final List<BoundaryLink> boundary = scanBoundary(link, maxCount);
                    result.add(Collections.unmodifiableList(boundary));
                }
            }
        }
        return result;
    }

    // This method is saved for debugging needs
    private List<HorizontalSection> doFindHorizontalSectionsSlowly() {
        assert !frames.isEmpty();
        final List<HorizontalSection> result = new ArrayList<HorizontalSection>();
        final HorizontalIBracketSet<HorizontalSideSeries> bracketSet =
            new HorizontalIBracketSet<HorizontalSideSeries>(horizontalSideSeries, false);
        HorizontalSection lastSection = null;
        while (bracketSet.next()) {
            if (bracketSet.horizontal.first && bracketSet.horizontal.containsBoundary()) {
                // We should remember that the current section of the whole figure (union)
                // at this horizontal is always, at least, a superset of the current side series
                // "bracketSet.horizontal"  (of course, the whole series lies non-strictly inside the union).
                if (lastSection != null && bracketSet.coord == lastSection.coord) {
                    assert lastSection.from() <= bracketSet.horizontal.from : "sides series sorted incorrectly";
                    if (bracketSet.horizontal.to <= lastSection.to()) {
                        // We did not leave the previous section: joning to previous section
                        lastSection.boundaryLinksAtSection.addAll(bracketSet.horizontal.containedBoundaryLinks);
                        continue;
                    }
                }
                final FrameSide left = bracketSet.maxLeftBeloningToUnion();
                final FrameSide right = bracketSet.minRightBeloningToUnion();
                assert left != null;
                assert right != null;
                assert left.coord() <= right.coord();
                // The range left..right is the current horizontal section
                lastSection = new HorizontalSection(true, bracketSet.coord, left, right);
                lastSection.boundaryLinksAtSection.addAll(bracketSet.horizontal.containedBoundaryLinks);
                result.add(lastSection);
            }
        }
        return result;
    }

    private List<HorizontalSection> doFindHorizontalSections() {
        assert !frames.isEmpty();
        final List<HorizontalSection> result = new ArrayList<HorizontalSection>();
        final HorizontalBoundaryIBracketSet<HorizontalBoundaryLink> bracketSet =
            new HorizontalBoundaryIBracketSet<HorizontalBoundaryLink>(allHorizontalBoundaryLinks());
        HorizontalSection lastSection = null;
        while (bracketSet.next()) {
            if (bracketSet.horizontal.atFirstOfTwoParallelSides()) {
                // We should remember that the current section of the whole figure (union)
                // at this horizontal is always, at least, a superset of the current side series
                // "bracketSet.horizontal"  (of course, the whole series lies non-strictly inside the union).
                if (lastSection != null && bracketSet.coord == lastSection.coord) {
                    assert lastSection.from() <= bracketSet.horizontal.from : "sides series sorted incorrectly";
                    if (bracketSet.horizontal.to <= lastSection.to()) {
                        // We did not leave the previous section: joning to previous section
                        lastSection.boundaryLinksAtSection.add(bracketSet.horizontal);
                        continue;
                    }
                }
                final int leftIndex = bracketSet.maxLeftIndexBeloningToUnion();
                final int rightIndex = bracketSet.minRightIndexBeloningToUnion();
                assert leftIndex <= rightIndex;
                // The range left..right is the current horizontal section
                final long left = allDifferentXAtBoundary[leftIndex];
                final long right = allDifferentXAtBoundary[rightIndex];
                lastSection = new HorizontalSection(true, bracketSet.coord, left, right, leftIndex, rightIndex);
                lastSection.boundaryLinksAtSection.add(bracketSet.horizontal);
                result.add(lastSection);
            }
        }
        return result;
    }

    private List<List<HorizontalBoundaryLink>> doFindClosingLinksIntersectingEachVertical() {
        assert !frames.isEmpty();
        final List<List<HorizontalBoundaryLink>> result = createListOfLists(allDifferentXAtBoundary.length - 1);
        for (HorizontalSideSeries series : horizontalSideSeriesAtBoundary) {
            assert series.containsBoundary();
            if (!series.first) {
                for (HorizontalBoundaryLink closingLink : series.containedBoundaryLinks) {
                    final int from = closingLink.transversalSeriesFrom.numberOfLessCoordinatesAtBoundary;
                    final int to = closingLink.transversalSeriesTo.numberOfLessCoordinatesAtBoundary;
                    for (int k = from; k < to; k++) {
                        result.get(k).add(closingLink);
                    }
                }
            }
        }
        return result;
    }

    private SearchIRectangleInHypograph doFindLargestRegtangles(
        List<HorizontalSection> horizontalSectionsByLowerSides,
        List<List<HorizontalBoundaryLink>> closingLinksIntersectingEachVertical)
    {
        assert !frames.isEmpty();
        assert allDifferentXAtBoundary.length >= 2 : "If frames exist, they cannot have <2 vertical boundary links";
        final SearchIRectangleInHypograph searcher = new SearchIRectangleInHypograph(allDifferentXAtBoundary);
        final long[] workY = new long[allDifferentXAtBoundary.length - 1];
        for (HorizontalSection section : horizontalSectionsByLowerSides) {
            searcher.setCurrentFromY(section.coord);
            for (HorizontalBoundaryLink openingLink : section.boundaryLinksAtSection) {
                // Usually 1-2 horizontal links.
                // We need to recalculate the hypograph above these links;
                // above other parts of this section it was recalculated before this.
                final int from = openingLink.transversalSeriesFrom.numberOfLessCoordinatesAtBoundary;
                final int to = openingLink.transversalSeriesTo.numberOfLessCoordinatesAtBoundary;
                Arrays.fill(workY, from, to, Long.MAX_VALUE);
                for (int k = from; k < to; k++) {
                    for (HorizontalBoundaryLink closingLink : closingLinksIntersectingEachVertical.get(k)) {
                        final long y = closingLink.coord();
                        if (y >= section.coord && y < workY[k]) {
                            // Important: >=, not >! The can be second (closing) links
                            // exactly at the same horizontal.
                            workY[k] = y;
                        }
                        // It is interesting that we can just ignore here all first (opening)
                        // horizontal links: inside all the horizontal section, which we should
                        // analyse, the next horizontal openingLink is always second (closing)
                    }
                }
                for (int k = from; k < to; k++) {
                    searcher.setY(k, workY[k]);
                }
            }
            if (DEBUG_LEVEL >= 3) {
                // Warning: it leads to incorrect global largest rectangle!
                searcher.resetAlreadyFoundRectangle();
            }
            searcher.resetMaxRectangleCorrected();
            searcher.correctMaximalRectangle(
                section.leftNumberOfLessCoordinatesAtBoundary,
                section.rightNumberOfLessCoordinatesAtBoundary);
            // no special action required for removed horizontal links
            if (searcher.isMaxRectangleCorrected()) {
                section.largestRectangle = searcher.largestRectangle();
            }
        }
        return searcher;
    }


    static void debug(int level, String format, Object... args) {
        if (DEBUG_LEVEL >= level) {
            System.out.printf(Locale.US, " IRU " + format, args);
        }
    }

    private static void addConnection(List<List<Frame>> connectionLists, Frame a, Frame b) {
        connectionLists.get(a.index).add(b);
        connectionLists.get(b.index).add(a);
        // If we even adds a connection twice, it is not a problem for searching connected components.
    }

    private static void addHorizontalLink(
        HorizontalIBracketSet<HorizontalSideSeries> bracketSet,
        FrameSide firstTransveral,
        FrameSide secondTransveral)
    {
        assert firstTransveral.coord() <= secondTransveral.coord();
        final HorizontalBoundaryLink link = new HorizontalBoundaryLink(
            bracketSet.horizontal,
            (VerticalSideSeries) firstTransveral.parentSeries,
            (VerticalSideSeries) secondTransveral.parentSeries);
        if (link.from < link.to) {
            if (DEBUG_LEVEL >= 3) {
                debug(3, "    adding %s%n", link);
            }
            bracketSet.horizontal.addLink(link);
        }
    }

    private static List<BoundaryLink> scanBoundary(HorizontalBoundaryLink start, long maxCount) {
        List<BoundaryLink> result = new ArrayList<BoundaryLink>();
        HorizontalBoundaryLink hLink = start;
        VerticalBoundaryLink vLink = start.linkTo;
        long count = 0;
        do {
            assert vLink.linkFrom != null && vLink.linkTo != null;
            assert hLink == vLink.linkFrom || hLink == vLink.linkTo;
            assert !hLink.joinedIntoAllBoundaries;
            assert !vLink.joinedIntoAllBoundaries;
            result.add(hLink);
            result.add(vLink);
            hLink.joinedIntoAllBoundaries = true;
            vLink.joinedIntoAllBoundaries = true;
            hLink = hLink == vLink.linkFrom ? vLink.linkTo : vLink.linkFrom;
            assert hLink.linkFrom != null && hLink.linkTo != null;
            vLink = vLink == hLink.linkFrom ? hLink.linkTo : hLink.linkFrom;
            if (++count > maxCount) {
                throw new AssertionError("Infinite loop detected while scanning the boundary");
            }
        } while (hLink != start);
        return result;
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
        long t1 = System.nanoTime();
        List<Frame> result = new ArrayList<Frame>();
        int index = 0;
        for (IRectangularArea rectangle : rectangles) {
            result.add(new Frame(rectangle, index++));
        }
        long t2 = System.nanoTime();
        debug(1, "Rectangle union (%d rectangles), initial allocating frames: %.3f ms%n",
            result.size(), (t2 - t1) * 1e-6);
        return result;
    }

    private static List<Frame> cloneFrames(Collection<Frame> frames) {
        assert frames != null;
        long t1 = System.nanoTime();
        List<Frame> result = new ArrayList<Frame>();
        int index = 0;
        for (Frame frame : frames) {
            result.add(new Frame(frame.rectangle, index++));
            // It is necessary to create new fields of Frame objects:
            // lessHorizontalSide, higherHorizontalSide, lessVerticalSide, higherVerticalSide
            // The current values of these fields provide access to SideSeries (parentSeries field)
            // and then to lists of links, which have no relation to newly created union.
        }
        long t2 = System.nanoTime();
        debug(1, "Rectangle union (%d rectangles), initial cloning frames: %.3f ms%n",
            result.size(), (t2 - t1) * 1e-6);
        return result;
    }

    private static <T> List<List<T>> createListOfLists(int n) {
        final List<List<T>> result = new ArrayList<List<T>>();
        for (int k = 0; k < n; k++) {
            result.add(new ArrayList<T>());
        }
        return result;
    }

    private static <T> long totalCount(List<List<T>> listOfLists) {
        long result = 0;
        for (List<T> list : listOfLists) {
            result += list.size();
        }
        return result;
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
        private final FrameSide firstSide;
        private List<FrameSide> otherSides = null;
        int indexInSortedListAtBoundary = -1;
        int numberOfLessCoordinatesAtBoundary = -1;

        private SideSeries(FrameSide initialSide) {
            super(initialSide.first);
            this.coord = initialSide.coord();
            this.from = initialSide.from();
            this.to = initialSide.to();
            this.sideFrom = initialSide.transversalFrameSideFrom();
            this.sideTo = initialSide.transversalFrameSideTo();
            this.firstSide = initialSide;
            initialSide.parentSeries = this;
        }

        @Override
        public long coord() {
            return coord;
        }

        @Override
        public long from() {
            return from;
        }

        @Override
        public long to() {
            return to;
        }

        @Override
        void allContainedFrameSides(List<FrameSide> result) {
            result.clear();
            result.add(firstSide);
            if (otherSides != null) {
                result.addAll(otherSides);
            }
        }

        @Override
        FrameSide transversalFrameSideFrom() {
            return sideFrom;
        }

        @Override
        FrameSide transversalFrameSideTo() {
            return sideTo;
        }

        boolean expand(FrameSide followingSide) {
            if (followingSide.isHorizontal() != this.isHorizontal()
                || followingSide.coord() != this.coord()
                || followingSide.first != this.first)
            {
                return false;
            }
            final long followingFrom = followingSide.from();
            final long followingTo = followingSide.to();
            if (followingFrom > to || followingTo < from) {
                return false;
            }
            if (followingFrom < from) {
                from = followingFrom;
                sideFrom = followingSide.transversalFrameSideFrom();
            }
            if (followingTo > to) {
                to = followingTo;
                sideTo = followingSide.transversalFrameSideTo();
            }
            if (otherSides == null) {
                otherSides = new ArrayList<FrameSide>();
            }
            otherSides.add(followingSide);
            followingSide.parentSeries = this;
            return true;
        }
    }

    static class HorizontalSideSeries extends SideSeries {
        private List<HorizontalBoundaryLink> containedBoundaryLinks = null;
        // null means an emoty list: it saves memory and time for allocation
        // (in real applications most of series do not contain links usually)

        private HorizontalSideSeries(FrameSide initialSide) {
            super(initialSide);
        }

        @Override
        public boolean isHorizontal() {
            return true;
        }

        private void addLink(HorizontalBoundaryLink link) {
            if (containedBoundaryLinks == null) {
                containedBoundaryLinks = new ArrayList<HorizontalBoundaryLink>();
            }
            containedBoundaryLinks.add(link);
        }

        private boolean containsBoundary() {
            return containedBoundaryLinks != null;
        }

        private int containedLinksCount() {
            return containedBoundaryLinks == null ? 0 : containedBoundaryLinks.size();
        }
    }

    static class VerticalSideSeries extends SideSeries {
        private List<VerticalBoundaryLink> containedBoundaryLinks = null;
        private List<HorizontalBoundaryLink> intersectingBoundaryLinks = null;
        // null means an emoty list: it saves memory and time for allocation
        // (in real applications most of series do not contain links usually)

        private VerticalSideSeries(FrameSide initialSide) {
            super(initialSide);
        }

        @Override
        public boolean isHorizontal() {
            return false;
        }

        private void addLink(VerticalBoundaryLink link) {
            if (containedBoundaryLinks == null) {
                containedBoundaryLinks = new ArrayList<VerticalBoundaryLink>();
            }
            containedBoundaryLinks.add(link);
        }

        private void addIntersectingLink(HorizontalBoundaryLink link) {
            if (intersectingBoundaryLinks == null) {
                intersectingBoundaryLinks = new ArrayList<HorizontalBoundaryLink>();
            }
            intersectingBoundaryLinks.add(link);
        }

        private boolean containsBoundary() {
            return containedBoundaryLinks != null;
        }

        private int containedLinksCount() {
            return containedBoundaryLinks == null ? 0 : containedBoundaryLinks.size();
        }
    }

    static class HorizontalSection extends Side {
        private final long coord;
        private final long left;
        private final long right;
        private final int leftNumberOfLessCoordinatesAtBoundary;
        private final int rightNumberOfLessCoordinatesAtBoundary;
        private final List<HorizontalBoundaryLink> boundaryLinksAtSection = new ArrayList<HorizontalBoundaryLink>();
        private IRectangularArea largestRectangle = null;
        // - can be accessed via reflection for debugging needs

        private HorizontalSection(boolean first, long coord, FrameSide left, FrameSide right) {
            this(
                first,
                coord,
                left.coord(),
                right.coord(),
                left.parentSeries.numberOfLessCoordinatesAtBoundary,
                right.parentSeries.numberOfLessCoordinatesAtBoundary);
        }

        private HorizontalSection(
            boolean first,
            long coord,
            long left,
            long right,
            int leftNumberOfLessCoordinatesAtBoundary,
            int rightNumberOfLessCoordinatesAtBoundary)
        {
            super(first);
            this.coord = coord;
            this.left = left;
            this.right = right;
            this.leftNumberOfLessCoordinatesAtBoundary = leftNumberOfLessCoordinatesAtBoundary;
            this.rightNumberOfLessCoordinatesAtBoundary = rightNumberOfLessCoordinatesAtBoundary;
        }

        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public long coord() {
            return coord;
        }

        @Override
        public long from() {
            return left;
        }

        @Override
        public long to() {
            return right;
        }

        @Override
        void allContainedFrameSides(List<FrameSide> result) {
            throw new UnsupportedOperationException();
        }

        @Override
        FrameSide transversalFrameSideFrom() {
            throw new UnsupportedOperationException();
        }

        @Override
        FrameSide transversalFrameSideTo() {
            throw new UnsupportedOperationException();
        }
    }
}
