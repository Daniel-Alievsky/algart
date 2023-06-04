/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.stitching;

import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.math.RectangularArea;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Stitcher<P extends FramePosition> {
    private static final double[] EMPTY_DOUBLES = new double[0];

    private static final double MIN_USED_PART_FOR_PRELOADING = 0.3;

    private final int dimCount;
    private final StitchingMethod<P> stitchingMethod;
    private final List<Frame<P>> frames;

    private Stitcher(int dimCount, StitchingMethod<P> stitchingMethod, List<? extends Frame<P>> frames) {
        if (stitchingMethod == null)
            throw new NullPointerException("Null stitchingMethod argument");
        if (dimCount <= 0)
            throw new IllegalArgumentException("Zero or negative dimCount argument = " + dimCount);
        this.dimCount = dimCount;
        this.stitchingMethod = stitchingMethod;
        this.frames = new ArrayList<Frame<P>>(frames);
        checkFrameDimensions(dimCount, this.frames);
    }

    public static <P extends FramePosition> Stitcher<P> getInstance(int dimCount,
        StitchingMethod<P> stitchingMethod, List<? extends Frame<P>> frames)
    {
        return new Stitcher<P>(dimCount, stitchingMethod, frames);
    }

    public final int dimCount() {
        return dimCount;
    }

    public final StitchingMethod<P> stitchingMethod() {
        return stitchingMethod;
    }

    public List<Frame<P>> frames() {
        return Collections.unmodifiableList(frames);
    }

    public Stitcher<P> frames(List<? extends Frame<P>> newFrames) {
        return new Stitcher<P>(dimCount, stitchingMethod, newFrames);
    }

    public List<Frame<P>> actualFrames(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
        if (area.coordCount() != dimCount)
            throw new IllegalArgumentException("Illegal number of dimensions in area argument: "
                + area.coordCount() + " instead of " + dimCount);
        List<Frame<P>> result = new ArrayList<Frame<P>>();
        for (Frame<P> frame : frames) {
            if (frame.position().area().overlaps(area)) {
                result.add(frame);
            }
        }
        return result;
    }

    public <T extends PArray> Matrix<T> asStitched(Class<? extends T> requiredType, RectangularArea area) {
        if (requiredType == null)
            throw new NullPointerException("Null requiredType argument");
        List<Frame<P>> actualFrames = actualFrames(area);
        // area.coordCount() == dimCount() was checked by actualFrames
        boolean shiftPositions = shiftPositions(actualFrames);
        boolean integerShiftPositions = shiftPositions && integerOffsets(actualFrames, area.min());

        if (shiftPositions) {
            // optimization: instead of shifting results by area.min(), we just subtract area.min() from all positions
            List<Frame<P>> shiftedFrames = new ArrayList<Frame<P>>();
            for (Frame<P> frame : actualFrames) {
                Matrix<? extends PArray> m = frame.matrix();
                RectangularArea shiftedArea = frame.position().area().shift(area.min().symmetric());
                P position = castPosition(ShiftFramePosition.valueOf(shiftedArea));
                // here we are sure that P is ShiftFramePosition, in other case shiftPosition(...) cannot return true
                shiftedFrames.add(DefaultFrame.valueOf(m, position));
            }
            actualFrames = shiftedFrames;
            area = RectangularArea.valueOf(Point.origin(dimCount), area.size());
        }

        final long[] dimensions = area.size().toIntegerPoint().coordinates(); // truncated area sizes
        if (actualFrames.isEmpty() && stitchingMethod.simpleBehaviorForEmptySpace()) {
//            System.out.println("Degenerated stitching " + actualFrames.size() + " frames: " + actualFrames);
            return Matrices.asCoordFuncMatrix(
                ConstantFunc.getInstance(outsideValue(actualFrames)), requiredType, dimensions);
        }
//        System.out.println("Stitching " + actualFrames.size() + " frames"
//            + (shiftPositions ? " (shifting)" : "") + " into " + area + ": " + actualFrames);
        if (actualFrames.size() == 1 && stitchingMethod.simpleBehaviorForSingleFrame()) {
            Frame<P> singleFrame = actualFrames.get(0);
            Matrix<? extends PArray> m = singleFrame.matrix();
            FramePosition p = singleFrame.position();
            if (integerShiftPositions) {
//                System.out.println("1 frame branch: integer coordinates");
                Point o = p.area().min();
                IPoint localOffset = o.toRoundedPoint();
                assert o.equals(localOffset.toPoint());
                Matrix<T> casted = Matrices.asFuncMatrix(Func.IDENTITY, requiredType, m);
                // - must be before subMatr, if requiredType allows outsideValue, but the source matrix does not
                return casted.subMatr(localOffset.symmetric().coordinates(), dimensions,
                    Matrix.ContinuationMode.getConstantMode(outsideValue(actualFrames)));
            }
            if (p instanceof UniversalFramePosition
                && ((UniversalFramePosition)p).inverseTransform() instanceof LinearOperator)
            {
                LinearOperator inverseTransform = (LinearOperator)((UniversalFramePosition)p).inverseTransform();
//                System.out.println("1 frame branch: linear operator " + inverseTransform);
                LinearOperator shift = LinearOperator.getShiftInstance(area.min().coordinates());
                LinearOperator lo = shift.superposition(inverseTransform);
                Func f = Matrices.asInterpolationFunc(m, Matrices.InterpolationMethod.POLYLINEAR_FUNCTION,
                    outsideValue(actualFrames));
                f = lo.apply(f);
                return Matrices.asCoordFuncMatrix(f, requiredType, dimensions);
            }
        }
        if (integerShiftPositions && stitchingMethod instanceof CoordinateFreeStitchingMethod<?>) {
//            System.out.println("coordinate free branch: combining submatrices");
            // optimization: here we can just create corresponded submatrices
            List<Matrix<? extends PArray>> expandedMatrices =
                new ArrayList<Matrix<? extends PArray>>(actualFrames.size());
            for (Frame<P> localFrame : actualFrames) {
                Point o = localFrame.position().area().min();
                IPoint localOffset = o.toRoundedPoint();
                assert o.equals(localOffset.toPoint());
                Matrix<? extends PArray> m = localFrame.matrix();
                m = Matrices.asFuncMatrix(Func.IDENTITY, DoubleArray.class, m);
                // - we need Double virtual matrix to provide correct NaN values to the combining function
                m = m.subMatr(localOffset.symmetric().coordinates(), dimensions, Matrix.ContinuationMode.NAN_CONSTANT);
                expandedMatrices.add(m);
            }
            return Matrices.asFuncMatrix(
                ((CoordinateFreeStitchingMethod<?>) stitchingMethod).combiningFunc(),
                requiredType, expandedMatrices);
        }
//        System.out.println("common branch: combining frames");
        return Matrices.asCoordFuncMatrix(getCombiner(actualFrames, area.min()), requiredType, dimensions);
    }

    public void stitch(ArrayContext context,  Matrix<? extends UpdatablePArray> result,
        Point offset, long... tileDimensions)
    {
        if (result == null)
            throw new NullPointerException("Null result argument");
        if (offset == null)
            throw new NullPointerException("Null offset argument");
        if (tileDimensions == null)
            throw new NullPointerException("Null tileDimensions argument");
        final int n = offset.coordCount();
        final long[] dim = result.dimensions();
        if (dim.length != n)
            throw new IllegalArgumentException("Different offset.coordCount() = " + n
                + " and result.dimCount() = " + dim.length);
        if (tileDimensions.length != n)
            throw new IllegalArgumentException("Different offset.coordCount() = " + n
                + " and tileDimensions.length = " + tileDimensions.length);
        long[] tileDim = new long[n];
        for (int k = 0; k < n; k++) {
            tileDim[k] = tileDimensions[k] <= 0 ? dim[k] : tileDimensions[k];
        }
        long[] tileCounts = new long[n];
        for (int k = 0; k < n; k++) {
            tileCounts[k] = dim[k] == 0 ? 0 : (dim[k] - 1) / tileDim[k] + 1;
        }
        long[] tileIndexes = new long[n];
        long[] subPos = new long[n];
        long[] subDim = new long[n];
        double[] subMin = new double[n];
        double[] subMax = new double[n];
        final Matrix<?> enumerator = Matrices.matrix(Arrays.nIntCopies(Arrays.longMul(tileCounts), 157), tileCounts);
        // - This trivial virtual matrix is a simplest way to enumerate all tiles.
        // Overflow is impossible in longMul here: all tileDim[k] > 0 if dim[k] >= 0, so tileCounts[k] <= dimParent[k]
        for (long tileIndex = 0, tileCount = enumerator.size(); tileIndex < tileCount; tileIndex++) {
            ArrayContext ac = context == null ? null : context.part(tileIndex, tileIndex + 1, tileCount);
            enumerator.coordinates(tileIndex, tileIndexes);
            for (int k = 0; k < n; k++) {
                subPos[k] = tileIndexes[k] * tileDim[k];
                assert subPos[k] < dim[k];
                subDim[k] = Math.min(tileDim[k], dim[k] - subPos[k]);
                subMin[k] = offset.coord(k) + subPos[k];
                subMax[k] = subMin[k] + subDim[k];
            }
            final RectangularArea subArea = RectangularArea.valueOf(Point.valueOf(subMin), Point.valueOf(subMax));
            final Matrix<? extends UpdatablePArray> subResult = result.subMatr(subPos, subDim);
            final List<Frame<P>> localFrames = actualFrames(subArea);
            Stitcher<P> localStitcher = frames(localFrames);
            // reduce the number of frames in stitcher as early as possible
            final double requiredMemory = sizeOfFrames(localFrames);
            if (!localStitcher.quickStitching(subArea)
                && requiredMemory <= Arrays.SystemSettings.maxTempJavaMemory()
                && Matrices.sizeOf(subResult) >= MIN_USED_PART_FOR_PRELOADING * requiredMemory)
            {
//                System.out.println(subArea + " downloaded into RAM");
                localStitcher = localStitcher.cloneIntoJavaMemory(ac == null ? null : ac.part(0.0, 0.3));
                ac = ac == null ? null : ac.part(0.3, 1.0);
//            } else {
//                System.out.println(subArea + " NOT downloaded into RAM");
            }
            final Matrix<? extends PArray> lazy = localStitcher.asStitched(result.type(PArray.class), subArea);
//            long t1 = System.nanoTime();
//            Arrays.CopyStatus copyStatus =
            Matrices.copy(ac, subResult, lazy, 0, false);
            localStitcher.freeResources(); // necessary if the stitched matrices occupy some resources
//            long t2 = System.nanoTime();
//            System.out.println(subArea + " (" + localFrames.size() + " frames) " + copyStatus
//                + " by " + Matrices.sizeOf(lazy) / 1048576 / (1e-9 * (t2 - t1)) + " MB/sec, "
//                + localStitcher + ", frames: " + localStitcher.frames());
        }
    }

    public Stitcher<P> cloneIntoJavaMemory(ArrayContext context) {
        return frames(cloneIntoJavaMemory(context, frames(), true));
    }

    public void freeResources() {
        for (Frame<?> frame : frames) {
            frame.freeResources();
        }
    }

    public boolean quickStitching(RectangularArea area) {
        final List<Frame<P>> actualFrames = actualFrames(area);
        return (stitchingMethod instanceof CoordinateFreeStitchingMethod<?>
            || (actualFrames.size() == 1 && stitchingMethod.simpleBehaviorForSingleFrame()))
            && shiftPositions(actualFrames)
            && integerOffsets(actualFrames, area.min());
    }

    @Override
    public String toString() {
        return "Stitcher by " + stitchingMethod + " of " + frames.size() + " frames";
    }

    public static <P extends FramePosition> boolean integerOffsets(List<Frame<P>> frames, Point offset) {
        for (Frame<?> frame : frames) {
            Point o = frame.position().area().min().subtract(offset);
            if (!o.equals(o.toRoundedPoint().toPoint())) {
                return false;
            }
        }
        return true;
    }

    public static <P extends FramePosition> boolean shiftPositions(List<Frame<P>> frames) {
        for (Frame<?> frame : frames) {
            FramePosition fp = frame.position();
            if (!(fp instanceof ShiftFramePosition)) {
                return false;
            }
        }
        return true;
    }

    public static <P extends FramePosition> double sizeOfFrames(List<Frame<P>> frames) {
        double result = 0.0;
        for (Frame<?> frame : frames) {
            result += Matrices.sizeOf(frame.matrix());
        }
        return result;
    }

    public static <P extends FramePosition> List<Frame<P>> cloneIntoJavaMemory(
        ArrayContext arrayContext, List<Frame<P>> frames, boolean freeSourceResources)
    {
        List<Frame<P>> result = new ArrayList<Frame<P>>(frames.size());
        for (int k = 0, n = frames.size(); k < n; k++) {
            ArrayContext ac = arrayContext == null ? null : arrayContext.part(k, k + 1, n);
            Frame<P> frame = frames.get(k);
            result.add(cloneIntoJavaMemory(ac, frame));
            if (freeSourceResources) { // necessary if the cloned matrices occupy some resources
                frame.freeResources();
            }
        }
        return result;
    }

    public static <P extends FramePosition> Frame<P> cloneIntoJavaMemory(ArrayContext arrayContext, Frame<P> frame) {
        Matrix<? extends UpdatablePArray> preloaded = Arrays.SMM.newMatrix(UpdatablePArray.class, frame.matrix());
        Matrices.copy(arrayContext, preloaded, frame.matrix());
        return DefaultFrame.valueOf(preloaded, frame.position());
    }

    private static void checkFrameDimensions(int dimCount, List<? extends Frame<?>> frames) {
        if (frames == null)
            throw new NullPointerException("Null frames argument");
        if (dimCount <= 0)
            throw new IllegalArgumentException("Zero or negative dimCount argument = " + dimCount);
        int n = 0;
        for (Frame<?> frame : frames) {
            if (frame == null)
                throw new NullPointerException("Null frame in the frames list");
            if (frame.matrix().dimCount() != dimCount)
                throw new IllegalArgumentException("frames.get(" + n + ") and frames.get(0) have "
                    + "different number of dimensions: frame #" + n + " is " + frame
                    + ", frame #0 is " + dimCount + "-dimensional");
            n++;
        }
        assert n == frames.size();
    }

    private double outsideValue(List<Frame<P>> stitchedFrames) {
        double[] probeCoordinates = Arrays.toJavaArray(Arrays.nDoubleCopies(dimCount(), 0.0));
        // (0,0,...) - ignored in SimpleStitchingMethod
        double[] probeValues = Arrays.toJavaArray(Arrays.nDoubleCopies(stitchedFrames.size(), Double.NaN));
        // all values are Double.NaN, that means the area outside frames
        return stitchingMethod.getStitchingFunc(stitchedFrames).get(probeCoordinates, probeValues);
    }

    @SuppressWarnings("unchecked")
    private P castPosition(FramePosition position) {
        return (P)position;
    }

    private Func getCombiner(List<Frame<P>> frames, Point offset) {
        if (offset.isOrigin()) {
            switch (frames.size()) {
                case 1:
                    return new CombinerFor1Frame(frames);
                case 2:
                    return new CombinerFor2Frames(frames);
                case 3:
                    return new CombinerFor3Frames(frames);
                case 4:
                    return new CombinerFor4Frames(frames);
                case 5:
                    return new CombinerFor5Frames(frames);
                case 6:
                    return new CombinerFor6Frames(frames);
                case 7:
                    return new CombinerFor7Frames(frames);
                case 8:
                    return new CombinerFor8Frames(frames);
                default:
                    return new Combiner(frames);
            }
        } else {
            switch (frames.size()) {
                case 1:
                    return new ShiftingCombinerFor1Frame(frames, offset);
                case 2:
                    return new ShiftingCombinerFor2Frames(frames, offset);
                case 3:
                    return new ShiftingCombinerFor3Frames(frames, offset);
                case 4:
                    return new ShiftingCombinerFor4Frames(frames, offset);
                case 5:
                    return new ShiftingCombinerFor5Frames(frames, offset);
                case 6:
                    return new ShiftingCombinerFor6Frames(frames, offset);
                case 7:
                    return new ShiftingCombinerFor7Frames(frames, offset);
                case 8:
                    return new ShiftingCombinerFor8Frames(frames, offset);
                default:
                    return new ShiftingCombiner(frames, offset);
            }
        }
    }

    private class Combiner implements Func {
        final StitchingFunc stitchingFunc;
        final Func[] interpolations;
        final Func interpolation0;
        final Func interpolation1;
        final Func interpolation2;
        final Func interpolation3;
        final Func interpolation4;
        final Func interpolation5;
        final Func interpolation6;
        final Func interpolation7;
        final int n;

        private Combiner(List<? extends Frame<P>> frames) {
            this.n = frames.size();
            this.stitchingFunc = stitchingMethod().getStitchingFunc(frames);
            Func[] interpolations = new Func[n];
            int k = 0;
            for (Frame<P> frame : frames) {
                interpolations[k] = frame.position().asInterpolationFunc(frame.matrix());
//                System.out.println(k + ": adding " + interpolations[k]);
                k++;
            }
            this.interpolations = interpolations;
            this.interpolation0 = n >= 1 ? interpolations[0] : null;
            this.interpolation1 = n >= 2 ? interpolations[1] : null;
            this.interpolation2 = n >= 3 ? interpolations[2] : null;
            this.interpolation3 = n >= 4 ? interpolations[3] : null;
            this.interpolation4 = n >= 5 ? interpolations[4] : null;
            this.interpolation5 = n >= 6 ? interpolations[5] : null;
            this.interpolation6 = n >= 7 ? interpolations[6] : null;
            this.interpolation7 = n >= 8 ? interpolations[7] : null;
        }

        public double get(double... x) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x);
            }
            return stitchingFunc.get(x, values);
        }

        public double get() {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(EMPTY_DOUBLES);
            }
            return stitchingFunc.get(EMPTY_DOUBLES, values);
        }

        public double get(double x0) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0);
            }
            return stitchingFunc.get1D(x0, values);
        }

        public double get(double x0, double x1) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0, x1);
            }
            return stitchingFunc.get2D(x0, x1, values);
        }

        public double get(double x0, double x1, double x2) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0, x1, x2);
            }
            return stitchingFunc.get3D(x0, x1, x2, values);
        }

        public double get(double x0, double x1, double x2, double x3) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0, x1, x2, x3);
            }
            return stitchingFunc.get(new double[] {x0, x1, x2, x3}, values);
        }

        /**
         * Returns a brief string description of this object.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            return "stitching combiner with " + stitchingFunc;
        }
    }

    /*Repeat() 1Frame ==> 2Frames,,3Frames,,4Frames,,5Frames,,6Frames,,7Frames,,8Frames;;
               1-frame ==> 2-frames,,3-frames,,4-frames,,5-frames,,6-frames,,7-frames,,8-frames;;
               (double\s+)v0(\s*=\s*interpolatio)n0(\.get.*?(?:\r(?!\n)|\n|\r\n)\s*) ==>
               $1v0$2n0$3$1v1$2n1$3,,$1v0$2n0$3$1v1$2n1$3$1v2$2n2$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3,,$1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3$1v6$2n6$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3$1v6$2n6$3$1v7$2n7$3;;
               v0\) ==> v0, v1),,v0, v1, v2),,v0, v1, v2, v3),,v0, v1, v2, v3, v4),,
               v0, v1, v2, v3, v4, v5),,v0, v1, v2, v3, v4, v5, v6),,v0, v1, v2, v3, v4, v5, v6, v7)
    */
    private class CombinerFor1Frame extends Combiner {
        private CombinerFor1Frame(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            return stitchingFunc.get(x, v0);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            return stitchingFunc.get1D(x0, v0);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0);
        }

        @Override
        public String toString() {
            return "1-frame stitching combiner with " + stitchingFunc;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private class CombinerFor2Frames extends Combiner {
        private CombinerFor2Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            return stitchingFunc.get(x, v0, v1);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            return stitchingFunc.get1D(x0, v0, v1);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1);
        }

        @Override
        public String toString() {
            return "2-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor3Frames extends Combiner {
        private CombinerFor3Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            return stitchingFunc.get(x, v0, v1, v2);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2);
        }

        @Override
        public String toString() {
            return "3-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor4Frames extends Combiner {
        private CombinerFor4Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            double v3 = interpolation3.get(x);
            return stitchingFunc.get(x, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3);
        }

        @Override
        public String toString() {
            return "4-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor5Frames extends Combiner {
        private CombinerFor5Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            double v3 = interpolation3.get(x);
            double v4 = interpolation4.get(x);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4);
        }

        @Override
        public String toString() {
            return "5-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor6Frames extends Combiner {
        private CombinerFor6Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            double v3 = interpolation3.get(x);
            double v4 = interpolation4.get(x);
            double v5 = interpolation5.get(x);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public String toString() {
            return "6-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor7Frames extends Combiner {
        private CombinerFor7Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            double v3 = interpolation3.get(x);
            double v4 = interpolation4.get(x);
            double v5 = interpolation5.get(x);
            double v6 = interpolation6.get(x);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            double v6 = interpolation6.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            double v6 = interpolation6.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            double v6 = interpolation6.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public String toString() {
            return "7-frames stitching combiner with " + stitchingFunc;
        }
    }

    private class CombinerFor8Frames extends Combiner {
        private CombinerFor8Frames(List<Frame<P>> frames) {
            super(frames);
        }

        @Override
        public double get(double... x) {
            double v0 = interpolation0.get(x);
            double v1 = interpolation1.get(x);
            double v2 = interpolation2.get(x);
            double v3 = interpolation3.get(x);
            double v4 = interpolation4.get(x);
            double v5 = interpolation5.get(x);
            double v6 = interpolation6.get(x);
            double v7 = interpolation7.get(x);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0) {
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            double v6 = interpolation6.get(x0);
            double v7 = interpolation7.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0, double x1) {
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            double v6 = interpolation6.get(x0, x1);
            double v7 = interpolation7.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            double v6 = interpolation6.get(x0, x1, x2);
            double v7 = interpolation7.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public String toString() {
            return "8-frames stitching combiner with " + stitchingFunc;
        }
    }
    /*Repeat.AutoGeneratedEnd*/

    private class ShiftingCombiner implements Func {
        final StitchingFunc stitchingFunc;
        final Func[] interpolations;
        final Func interpolation0;
        final Func interpolation1;
        final Func interpolation2;
        final Func interpolation3;
        final Func interpolation4;
        final Func interpolation5;
        final Func interpolation6;
        final Func interpolation7;
        final double[] offset;
        final double offset0;
        final double offset1;
        final double offset2;
        final double offset3;
        final int n;

        private ShiftingCombiner(List<Frame<P>> frames, Point offset) {
            this.n = frames.size();
            this.stitchingFunc = stitchingMethod().getStitchingFunc(frames);
            Func[] interpolations = new Func[n];
            int k = 0;
            for (Frame<P> frame : frames) {
                interpolations[k] = frame.position().asInterpolationFunc(frame.matrix());
//                System.out.println(k + ": adding " + interpolations[k]);
                k++;
            }
            this.interpolations = interpolations;
            this.interpolation0 = n >= 1 ? interpolations[0] : null;
            this.interpolation1 = n >= 2 ? interpolations[1] : null;
            this.interpolation2 = n >= 3 ? interpolations[2] : null;
            this.interpolation3 = n >= 4 ? interpolations[3] : null;
            this.interpolation4 = n >= 5 ? interpolations[4] : null;
            this.interpolation5 = n >= 6 ? interpolations[5] : null;
            this.interpolation6 = n >= 7 ? interpolations[6] : null;
            this.interpolation7 = n >= 8 ? interpolations[7] : null;
            this.offset = offset.coordinates();
            this.offset0 = this.offset.length >= 1 ? this.offset[0] : Double.NaN;
            this.offset1 = this.offset.length >= 2 ? this.offset[1] : Double.NaN;
            this.offset2 = this.offset.length >= 3 ? this.offset[2] : Double.NaN;
            this.offset3 = this.offset.length >= 4 ? this.offset[3] : Double.NaN;
        }

        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(shifted);
            }
            return stitchingFunc.get(shifted, values);
        }

        public double get() {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(EMPTY_DOUBLES);
            }
            return stitchingFunc.get(EMPTY_DOUBLES, values);
        }

        public double get(double x0) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0 + offset0);
            }
            return stitchingFunc.get1D(x0 + offset0, values);
        }

        public double get(double x0, double x1) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0 + offset0, x1 + offset1);
            }
            return stitchingFunc.get2D(x0 + offset0, x1 + offset1, values);
        }

        public double get(double x0, double x1, double x2) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0 + offset0, x1 + offset1, x2 + offset2);
            }
            return stitchingFunc.get3D(x0 + offset0, x1 + offset1, x2 + offset2, values);
        }

        public double get(double x0, double x1, double x2, double x3) {
            double[] values = new double[n];
            for (int k = 0; k < n; k++) {
                values[k] = interpolations[k].get(x0 + offset0, x1 + offset1, x2 + offset2, x3 + offset3);
            }
            return stitchingFunc.get(new double[] {x0 + offset0, x1 + offset1, x2 + offset2, x3 + offset3}, values);
        }

        /**
         * Returns a brief string description of this object.
         *
         * @return a brief string description of this object.
         */
        @Override
        public String toString() {
            return "shifting stitching combiner with " + stitchingFunc;
        }
    }

    /*Repeat() 1Frame ==> 2Frames,,3Frames,,4Frames,,5Frames,,6Frames,,7Frames,,8Frames;;
               1-frame ==> 2-frames,,3-frames,,4-frames,,5-frames,,6-frames,,7-frames,,8-frames;;
               (double\s+)v0(\s*=\s*interpolatio)n0(\.get.*?(?:\r(?!\n)|\n|\r\n)\s*) ==>
               $1v0$2n0$3$1v1$2n1$3,,$1v0$2n0$3$1v1$2n1$3$1v2$2n2$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3,,$1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3$1v6$2n6$3,,
               $1v0$2n0$3$1v1$2n1$3$1v2$2n2$3$1v3$2n3$3$1v4$2n4$3$1v5$2n5$3$1v6$2n6$3$1v7$2n7$3;;
               v0\) ==> v0, v1),,v0, v1, v2),,v0, v1, v2, v3),,v0, v1, v2, v3, v4),,
               v0, v1, v2, v3, v4, v5),,v0, v1, v2, v3, v4, v5, v6),,v0, v1, v2, v3, v4, v5, v6, v7)
    */
    private class ShiftingCombinerFor1Frame extends ShiftingCombiner {
        private ShiftingCombinerFor1Frame(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            return stitchingFunc.get(x, v0);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            return stitchingFunc.get1D(x0, v0);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0);
        }

        @Override
        public String toString() {
            return "1-frame shifting stitching combiner with " + stitchingFunc;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private class ShiftingCombinerFor2Frames extends ShiftingCombiner {
        private ShiftingCombinerFor2Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            return stitchingFunc.get(x, v0, v1);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            return stitchingFunc.get1D(x0, v0, v1);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1);
        }

        @Override
        public String toString() {
            return "2-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor3Frames extends ShiftingCombiner {
        private ShiftingCombinerFor3Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2);
        }

        @Override
        public String toString() {
            return "3-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor4Frames extends ShiftingCombiner {
        private ShiftingCombinerFor4Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            double v3 = interpolation3.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3);
        }

        @Override
        public String toString() {
            return "4-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor5Frames extends ShiftingCombiner {
        private ShiftingCombinerFor5Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            double v3 = interpolation3.get(shifted);
            double v4 = interpolation4.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4);
        }

        @Override
        public String toString() {
            return "5-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor6Frames extends ShiftingCombiner {
        private ShiftingCombinerFor6Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            double v3 = interpolation3.get(shifted);
            double v4 = interpolation4.get(shifted);
            double v5 = interpolation5.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5);
        }

        @Override
        public String toString() {
            return "6-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor7Frames extends ShiftingCombiner {
        private ShiftingCombinerFor7Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            double v3 = interpolation3.get(shifted);
            double v4 = interpolation4.get(shifted);
            double v5 = interpolation5.get(shifted);
            double v6 = interpolation6.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            double v6 = interpolation6.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            double v6 = interpolation6.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            double v6 = interpolation6.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5, v6);
        }

        @Override
        public String toString() {
            return "7-frames shifting stitching combiner with " + stitchingFunc;
        }
    }

    private class ShiftingCombinerFor8Frames extends ShiftingCombiner {
        private ShiftingCombinerFor8Frames(List<Frame<P>> frames, Point offset) {
            super(frames, offset);
        }

        @Override
        public double get(double... x) {
            double[] shifted = new double[x.length];
            for (int k = 0; k < x.length; k++) {
                shifted[k] = x[k] + offset[k];
            }
            double v0 = interpolation0.get(shifted);
            double v1 = interpolation1.get(shifted);
            double v2 = interpolation2.get(shifted);
            double v3 = interpolation3.get(shifted);
            double v4 = interpolation4.get(shifted);
            double v5 = interpolation5.get(shifted);
            double v6 = interpolation6.get(shifted);
            double v7 = interpolation7.get(shifted);
            return stitchingFunc.get(x, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0) {
            x0 += offset0;
            double v0 = interpolation0.get(x0);
            double v1 = interpolation1.get(x0);
            double v2 = interpolation2.get(x0);
            double v3 = interpolation3.get(x0);
            double v4 = interpolation4.get(x0);
            double v5 = interpolation5.get(x0);
            double v6 = interpolation6.get(x0);
            double v7 = interpolation7.get(x0);
            return stitchingFunc.get1D(x0, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0, double x1) {
            x0 += offset0;
            x1 += offset1;
            double v0 = interpolation0.get(x0, x1);
            double v1 = interpolation1.get(x0, x1);
            double v2 = interpolation2.get(x0, x1);
            double v3 = interpolation3.get(x0, x1);
            double v4 = interpolation4.get(x0, x1);
            double v5 = interpolation5.get(x0, x1);
            double v6 = interpolation6.get(x0, x1);
            double v7 = interpolation7.get(x0, x1);
            return stitchingFunc.get2D(x0, x1, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public double get(double x0, double x1, double x2) {
            x0 += offset0;
            x1 += offset1;
            x2 += offset2;
            double v0 = interpolation0.get(x0, x1, x2);
            double v1 = interpolation1.get(x0, x1, x2);
            double v2 = interpolation2.get(x0, x1, x2);
            double v3 = interpolation3.get(x0, x1, x2);
            double v4 = interpolation4.get(x0, x1, x2);
            double v5 = interpolation5.get(x0, x1, x2);
            double v6 = interpolation6.get(x0, x1, x2);
            double v7 = interpolation7.get(x0, x1, x2);
            return stitchingFunc.get3D(x0, x1, x2, v0, v1, v2, v3, v4, v5, v6, v7);
        }

        @Override
        public String toString() {
            return "8-frames shifting stitching combiner with " + stitchingFunc;
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}
