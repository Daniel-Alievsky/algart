package net.algart.matrices.morphology;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.UniformGridPattern;
import net.algart.matrices.StreamingApertureProcessor;

import java.util.List;
import java.util.ArrayList;

abstract class RankOperationProcessor extends StreamingApertureProcessor {
    static final int BUFFER_BLOCK_SIZE = 65536;
    static boolean OPTIMIZE_GET_DATA = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.morphology.RankMorphology.optimizeGetData", true);
    static boolean OPTIMIZE_DIRECT_ARRAYS = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.morphology.RankMorphology.optimizeDirectArrays", true);
    static boolean INLINE_ONE_LEVEL = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.morphology.RankMorphology.inlineOneLevel", true);
    static boolean OPTIMIZE_SEGMENTS_ALONG_AXES = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.morphology.RankMorphology.optimizeSegmentsAlongAxes", true);
    static boolean SPECIAL_OPTIMIZE_THIN_PATTERNS_POWERS_OF_TWO = Arrays.SystemSettings.getBooleanProperty(
        "net.algart.matrices.morphology.RankMorphology.specialOptimizeThinPatternsPowersOfTwo", true);
    // - these constants are not final to allow changing via reflection in special tests

    final int[] bitLevels; // we never change them, so the standard cloning scheme is suitable
    final int numberOfAnalyzedBits;

    RankOperationProcessor(ArrayContext context, int[] bitLevels) {
        super(context);
        if (bitLevels == null)
            throw new NullPointerException("Null bitLevels argument");
        if (bitLevels.length == 0)
            throw new IllegalArgumentException("Empty bitLevels argument");
        bitLevels = bitLevels.clone();
        // clone before checking to guarantee correct check while multithreading
        Histogram.newIntHistogram(0, bitLevels); // checking bitLevels
        this.numberOfAnalyzedBits = bitLevels[bitLevels.length - 1];
        if (this.numberOfAnalyzedBits > CustomRankPrecision.MAX_NUMBER_OF_ANALYZED_BITS)
            throw new IllegalArgumentException("Last bitLevel is greater than "
                + CustomRankPrecision.MAX_NUMBER_OF_ANALYZED_BITS);
        this.bitLevels = JArrays.copyOfRange(bitLevels, 0, bitLevels.length - 1);
    }

    public final int[] bitLevels() {
        int[] result = new int[bitLevels.length + 1];
        System.arraycopy(bitLevels, 0, result, 0, bitLevels.length);
        result[bitLevels.length] = numberOfAnalyzedBits;
        return result;
    }

    @Override
    public final <T extends PArray> Matrix<T> asProcessed(Class<? extends T> requiredType,
        Matrix<? extends PArray> src,
        List<? extends Matrix<? extends PArray>> additionalMatrices,
        Pattern pattern)
    {
        if (additionalMatrices == null)
            throw new NullPointerException("Null additionalMatrices argument");
        additionalMatrices = new ArrayList<Matrix<? extends PArray>>(additionalMatrices);
        // - to avoid changing by parallel threads
        checkArguments(src, src, additionalMatrices, pattern);
        PArray srcArray = src.array();
        long[] dimensions = src.dimensions();
        PArray[] additionalArrays = new PArray[additionalMatrices.size()];
        for (int k = 0; k < additionalArrays.length; k++) {
            additionalArrays[k] = additionalMatrices.get(k).array();
        }
        long size = srcArray.length();
        long[] shifts = BasicMorphology.toShifts(null, size, dimensions, pattern, false);
        if (shifts.length == 0)
            throw new AssertionError("Empty pattern: it is impossible");
        UniformGridPattern roundedPattern = pattern.round();
        long[] leftShifts = BasicMorphology.toShifts(null, size, dimensions, roundedPattern.lowerSurface(0), false);
        long[] rightShifts = BasicMorphology.toShifts(null, size, dimensions, roundedPattern.upperSurface(0), false);
        if (leftShifts.length != rightShifts.length)
            throw new AssertionError("Unbalanced pattern.lowerSurface / pattern.upperSurface: different lengths");
        PArray processed = asProcessed(requiredType, srcArray, additionalArrays, dimensions,
            shifts, leftShifts, rightShifts);
        T result;
        if (processed.type() != requiredType) { // in particular, if requiredType is not one of 9 basic interfaces
            result = Arrays.asFuncArray(true, Func.IDENTITY, requiredType, processed);
        } else {
            result = requiredType.cast(processed);
        }
        return Matrices.matrix(result, dimensions);
    }

    // desiredType can be ignored: in any case, the result wil be casted to it.
    // But it is possible to optimize calculations by correct creating the array of necessary type.
    abstract PArray asProcessed(Class<? extends PArray> desiredType, PArray src, PArray[] additional,
        long[] dimensions, long[] shifts, long[] left, long[] right);
}
