package net.algart.matrices.stitching;

import java.util.List;

public interface StitchingMethod<P extends FramePosition> {

    public StitchingFunc getStitchingFunc(List<? extends Frame<P>> frames);

    public boolean simpleBehaviorForEmptySpace();

    public boolean simpleBehaviorForSingleFrame();
}
