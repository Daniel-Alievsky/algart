/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
