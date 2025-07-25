/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.math.RectangularArea;
import net.algart.math.functions.LinearOperator;

import java.util.Objects;

public final class ShiftFramePosition extends UniversalFramePosition implements FramePosition {
    ShiftFramePosition(RectangularArea area) {
        super(area, LinearOperator.getShiftInstance(area.min().symmetric().coordinates()));
    }

    public static ShiftFramePosition valueOf(Point offset, long... dimensions) {
        Objects.requireNonNull(offset, "Null offset argument");
        Objects.requireNonNull(dimensions, "Null dimensions argument");
        return new ShiftFramePosition(area(offset, dimensions));
    }

    public static ShiftFramePosition valueOf(RectangularArea area) {
        Objects.requireNonNull(area, "Null area argument");
        return new ShiftFramePosition(area);
    }

    public static RectangularArea area(Point offset, long[] dimensions) {
        return RectangularArea.valueOf(offset, offset.add(IPoint.valueOf(dimensions).toPoint()));
    }

    @Override
    public String toString() {
        return "shift frame position " + area();
    }

}
