package net.algart.matrices.stitching;

import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.math.RectangularArea;
import net.algart.math.functions.LinearOperator;

public final class ShiftFramePosition extends UniversalFramePosition implements FramePosition {
    ShiftFramePosition(RectangularArea area) {
        super(area, LinearOperator.getShiftInstance(area.min().symmetric().coordinates()));
    }

    public static ShiftFramePosition valueOf(Point offset, long... dimensions) {
        if (offset == null)
            throw new NullPointerException("Null offset argument");
        if (dimensions == null)
            throw new NullPointerException("Null dimensions argument");
        return new ShiftFramePosition(area(offset, dimensions));
    }

    public static ShiftFramePosition valueOf(RectangularArea area) {
        if (area == null)
            throw new NullPointerException("Null area argument");
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
