package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.patterns.Pattern;

class FixedPercentileRankMorphology extends BasicRankMorphology implements RankMorphology {
    private final double dilationLevel;

    FixedPercentileRankMorphology(ArrayContext context, double dilationLevel, boolean interpolated, int[] bitLevels) {
        super(context, interpolated, bitLevels);
        if (dilationLevel < 0.0 || dilationLevel > 1.0)
            throw new IllegalArgumentException("Illegal dilationLevel = " + dilationLevel
                + ": it must be in 0..1 range");
        this.dilationLevel = dilationLevel;
    }

    @Override
    protected Matrix<? extends PArray> asDilationOrErosion(Matrix<? extends PArray> src, Pattern pattern,
        boolean isDilation)
    {
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        long patternSize = pattern.pointCount();
        assert patternSize >= 1;
        double percentileIndex = (isDilation ? dilationLevel : 1.0 - dilationLevel) * (patternSize - 1);
        return asPercentile(src,
            interpolated || src.array() instanceof PFloatingArray ? percentileIndex : Math.round(percentileIndex),
            isDilation ? pattern : pattern.symmetric());
    }

    @Override
    protected Matrix<? extends UpdatablePArray> dilationOrErosion(
        Matrix<? extends UpdatablePArray> dest, Matrix<? extends PArray> src, Pattern pattern, boolean isDilation,
        boolean disableMemoryAllocation)
    {
        if (src == null)
            throw new NullPointerException("Null src argument");
        if (pattern == null)
            throw new NullPointerException("Null pattern argument");
        if (dest == null) {
            dest = memoryModel().newMatrix(UpdatablePArray.class, src);
        }
        long patternSize = pattern.pointCount();
        assert patternSize >= 1;
        double percentileIndex = (isDilation ? dilationLevel : 1.0 - dilationLevel) * (patternSize - 1);
        percentile(dest, src,
            interpolated || src.array() instanceof PFloatingArray ? percentileIndex : Math.round(percentileIndex),
            isDilation ? pattern : pattern.symmetric());
        return dest;
    }
}
