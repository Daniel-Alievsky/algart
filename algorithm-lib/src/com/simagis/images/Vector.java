package com.simagis.images;

import net.algart.arrays.PArray;

/**
 * This interface guarantees that {@link MatrixND} is really 1-dimensional.
 */
public interface Vector extends MatrixND {
    public long length();

    public PArray a();
}
