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

/**
 * <p>Set-theoretic union of several 2-dimensional {@link IRectangularArea rectangles}
 * with integer integer coordinates of vertices and sides, parallel to coordinate axes.
 * This class allows to solve the following main tasks:
 * <ol>
 * <li>find connected components in this union;</li>
 * <li>find its boundary as a polygon: a sequence of links, where each link is a horizontal or vertical
 * segment (1st link is horizontal, 2nd is vertical, 3rd is horizontal, etc.)</li>
 * </ol>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.
 * However, all important information is returned "lazily", i.e. while the 1st attempt to read it.
 * So, the instance creation method {@link #newInstance(Collection)}
 * works quickly and does not lead to complex calculations and allocating additional memory.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
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

        abstract List<FrameSide> allContainedFrameSides();

        abstract FrameSide transversalFrameSideFrom();

        abstract FrameSide transversalFrameSideTo();
    }

    public static abstract class FrameSide extends Side {
        final Frame frame;
        SideSeries containingSeries = null;
        // - creation of containingSeries is synchronized

        private FrameSide(boolean first, Frame frame) {
            super(first);
            assert frame != null;
            this.frame = frame;
        }

        public Frame frame() {
            return frame;
        }

        @Override
        List<FrameSide> allContainedFrameSides() {
            return Collections.singletonList(this);
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
        FrameSide transversalFrameSideFrom() {
            return frame.lessVerticalSide;
        }

        @Override
        FrameSide transversalFrameSideTo() {
            return frame.higherVerticalSide;
        }
    }

    public static abstract class BoundaryLink implements Comparable<BoundaryLink> {
        final SideSeries containingSeries;
        long from;
        long to;
        boolean joinedIntoAllBoundaries = false;

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

    public static class HorizontalBoundaryLink extends BoundaryLink {
        VerticalSideSeries transversalSeriesFrom;
        VerticalSideSeries transversalSeriesTo;
        // - these two fields are necessary only while constructing the boundary
        VerticalBoundaryLink linkFrom = null;
        VerticalBoundaryLink linkTo = null;

        private HorizontalBoundaryLink(
            SideSeries containingSeries,
            VerticalSideSeries transversalSeriesFrom,
            VerticalSideSeries transversalSeriesTo)
        {
            super(containingSeries,
                transversalSeriesFrom.boundCoord(),
                transversalSeriesTo.boundCoord());
            this.transversalSeriesFrom = transversalSeriesFrom;
            this.transversalSeriesTo = transversalSeriesTo;
        }

        @Override
        public IRectangularArea sidePart() {
            return IRectangularArea.valueOf(
                from, containingSeries.frameSideCoord(),
                to - 1, containingSeries.frameSideCoord());
        }

        void setNeighbour(VerticalBoundaryLink neighbour) {
            if (neighbour.containingSeries == transversalSeriesFrom) {
                linkFrom = neighbour;
            } else if (neighbour.containingSeries == transversalSeriesTo) {
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
            VerticalSideSeries containingSeries,
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
    private volatile List<VerticalSideSeries> verticalSideSeries = null;
    private volatile List<List<Frame>> connectedComponents = null;
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
        findBoundaries();
        final List<HorizontalBoundaryLink> result = new ArrayList<HorizontalBoundaryLink>();
        for (HorizontalSideSeries series : horizontalSideSeries) {
            result.addAll(series.containedBoundaryLinks);
        }
        return Collections.unmodifiableList(result);
    }

    public List<VerticalBoundaryLink> allVerticalBoundaryLinks() {
        findBoundaries();
        final List<VerticalBoundaryLink> result = new ArrayList<VerticalBoundaryLink>();
        for (VerticalSideSeries series : verticalSideSeries) {
            result.addAll(series.containedBoundaryLinks);
        }
        return Collections.unmodifiableList(result);
    }

    public List<List<BoundaryLink>> allBoundaries() {
        findBoundaries();
        return Collections.unmodifiableList(allBoundaries);
    }

    public void findConnectedComponents() {
        doCreateSideLists();
        synchronized (lock) {
            if (this.connectedComponents != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.connectedComponents = Collections.emptyList();
            }
        }
        long t1 = System.nanoTime();
        List<List<Frame>> connectionLists = createListOfLists(frames.size());
        long t2 = System.nanoTime();
        final long nConnections = doFillConnectionLists(connectionLists);
        long t3 = System.nanoTime();
        final List<List<Frame>> result = doFindConnectedComponents(connectionLists);
        long t4 = System.nanoTime();
        synchronized (lock) {
            this.connectedComponents = result;
        }
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

    public void findBoundaries() {
        doCreateSideLists();
        synchronized (lock) {
            // Global synchronization necessary, because we change internal fields like containingSeries
            // in already existing structures.
            if (this.allBoundaries != null) {
                return;
            }
            if (frames.isEmpty()) {
                this.allBoundaries = Collections.emptyList();
                this.horizontalSideSeries = Collections.emptyList();
                this.verticalSides = Collections.emptyList();
                return;
            }
            long t1 = System.nanoTime();
            this.horizontalSideSeries = createHorizontalSideSeriesLists();
            this.verticalSideSeries = createVerticalSideSeriesLists();
            long t2 = System.nanoTime();
            final long hCount = doFindHorizontalBoundaries();
            long t3 = System.nanoTime();
            final long vCount = doConvertHorizontalToVerticalLinks();
            if (vCount != hCount) {
                throw new AssertionError("Different number of horizontal and vertical links found");
            }
            long t4 = System.nanoTime();
            final List<List<BoundaryLink>> result = doJoinBoundaries(hCount);
            long t5 = System.nanoTime();
            synchronized (lock) {
                this.allBoundaries = result;
            }
            long t6 = System.nanoTime();
            long totalLinkCount = 0;
            for (List<BoundaryLink> boundary : result) {
                totalLinkCount += boundary.size();
            }
            debug(1, "Rectangle union (%d rectangles), finding %d boundaries with %d links: "
                    + "%.3f ms = %.3f ms initializing + %.3f ms %d horizontal links + %.3f ms %d vertical links + "
                    + "%.3f ms joining links + %.3f ms correcting data structures (%.3f mcs / rectangle)%n",
                frames.size(), result.size(), totalLinkCount,
                (t6 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, hCount, (t4 - t3) * 1e-6, vCount,
                (t5 - t4) * 1e-6, (t6 - t5) * 1e-6,
                (t6 - t1) * 1e-3 / (double) frames.size());
        }
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
                        assert bracket.covers(bracketSet.y);
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
                    System.out.printf("  Neighbours of %s:%n", frame);
                    for (Frame neighbour : neighbours) {
                        System.out.printf("    %s%n", neighbour);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    private List<List<Frame>> doFindConnectedComponentsDeprecated() {
        assert !frames.isEmpty();
        final List<List<Frame>> result = new ArrayList<List<Frame>>();
        if (DEBUG_LEVEL >= 4) {
            System.out.printf("  All verticals%n");
            for (VerticalSide side : verticalSides) {
                System.out.printf("    %s%n", side);
            }
        }
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
                doFindIncidentFramesDeprecated(neighbours, frame, allX, added);
                for (Frame neighbour : neighbours) {
                    if (!frameVisited[neighbour.index]) {
                        queue.add(neighbour);
                        frameVisited[neighbour.index] = true;
                    }
                }
                if (DEBUG_LEVEL >= 4) {
                    System.out.printf("  Neighbours of %s:%n", frame);
                    for (Frame neighbour : neighbours) {
                        System.out.printf("    %s%n", neighbour);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    private void doFindIncidentFramesDeprecated(List<Frame> result, Frame frame, long[] allX, boolean added[]) {
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
            System.out.printf("  %d side series:%n", sideSeries.size());
            for (int k = 0, n = sideSeries.size(); k < n; k++) {
                System.out.printf("    side series %d/%d: %s%n", k, n, sideSeries.get(k));
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
                    assert bracket.covers(bracketSet.y);
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
            count += bracketSet.horizontal.containedBoundaryLinks.size();
        }
        return count;
    }

    private long doConvertHorizontalToVerticalLinks() {
        assert !frames.isEmpty();
        for (HorizontalSideSeries series : horizontalSideSeries) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                link.transversalSeriesFrom.intersectingBoundaryLinks.add(link);
                link.transversalSeriesTo.intersectingBoundaryLinks.add(link);
            }
        }
        HorizontalBoundaryLink[] horizontalLinks = new HorizontalBoundaryLink[0];
        long count = 0;
        for (VerticalSideSeries verticalSeries : verticalSideSeries) {
            final int horizontalsCount = verticalSeries.intersectingBoundaryLinks.size();
            assert horizontalsCount % 2 == 0;
            if (horizontalsCount == 0) {
                continue;
            }
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
                verticalSeries.containedBoundaryLinks.add(link);
            }
            count += verticalSeries.containedBoundaryLinks.size();
        }
        return count;
    }

    private List<List<BoundaryLink>> doJoinBoundaries(long numberOfHorizontalLinks) {
        assert !frames.isEmpty();
        final long maxCount = 10 * Math.min(numberOfHorizontalLinks, Integer.MAX_VALUE);
        // really the limit is 2 * numberOfHorizontalLinks;
        // Integer.MAX_VALUE is also the limit due to 31-bit result.size()
        List<List<BoundaryLink>> result = new ArrayList<List<BoundaryLink>>();
        for (HorizontalSideSeries series : horizontalSideSeries) {
            for (HorizontalBoundaryLink link : series.containedBoundaryLinks) {
                if (!link.joinedIntoAllBoundaries) {
                    result.add(Collections.unmodifiableList(scanBoundary(link, maxCount)));
                }
            }
        }
        return result;
    }

    static void debug(int level, String format, Object... args) {
        if (DEBUG_LEVEL >= level) {
            System.out.printf(Locale.US, format, args);
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
        assert firstTransveral.boundCoord() <= secondTransveral.boundCoord();
        final HorizontalBoundaryLink link = new HorizontalBoundaryLink(
            bracketSet.horizontal,
            (VerticalSideSeries) firstTransveral.containingSeries,
            (VerticalSideSeries) secondTransveral.containingSeries);
        if (link.from < link.to) {
            if (DEBUG_LEVEL >= 3) {
                System.out.printf("    adding %s%n", link);
            }
            bracketSet.horizontal.containedBoundaryLinks.add(link);
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
        List<Frame> frames = new ArrayList<Frame>();
        int index = 0;
        for (IRectangularArea rectangle : rectangles) {
            frames.add(new Frame(rectangle, index++));
        }
        long t2 = System.nanoTime();
        debug(1, "Rectangle union (%d rectangles), initial allocating frames: %.3f ms%n",
            frames.size(), (t2 - t1) * 1e-6);
        return frames;
    }

    private static <T> List<List<T>> createListOfLists(int n) {
        final List<List<T>> result = new ArrayList<List<T>>();
        for (int k = 0; k < n; k++) {
            result.add(new ArrayList<T>());
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
        private List<FrameSide> sides = new ArrayList<FrameSide>();

        private SideSeries(FrameSide initialSide) {
            super(initialSide.first);
            this.coord = initialSide.boundCoord();
            this.from = initialSide.boundFrom();
            this.to = initialSide.boundTo();
            this.sideFrom = initialSide.transversalFrameSideFrom();
            this.sideTo = initialSide.transversalFrameSideTo();
            this.sides.add(initialSide);
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
        List<FrameSide> allContainedFrameSides() {
            return sides;
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
                || followingSide.boundCoord() != this.boundCoord()
                || followingSide.first != this.first)
            {
                return false;
            }
            final long followingFrom = followingSide.boundFrom();
            final long followingTo = followingSide.boundTo();
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
            this.sides.add(followingSide);
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
        List<HorizontalBoundaryLink> intersectingBoundaryLinks = new ArrayList<HorizontalBoundaryLink>();
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

