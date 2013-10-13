package net.algart.arrays;

/**
 * <p>AlgART array of any primitive numeric elements (byte, short, int, long, float or double),
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray},
 * {@link FloatArray}, {@link DoubleArray} subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface PNumberArray extends PArray {
    public Class<? extends PNumberArray> type();

    public Class<? extends UpdatablePNumberArray> updatableType();

    public Class<? extends MutablePNumberArray> mutableType();
}
