package net.algart.arrays;

/**
 * <p>AlgART array of any floating-point primitive elements (float or double), read/write access, no resizing.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link UpdatableFloatArray}, {@link UpdatableDoubleArray} subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatablePFloatingArray extends PFloatingArray, UpdatablePNumberArray {
    public UpdatablePFloatingArray subArray(long fromIndex, long toIndex);

    public UpdatablePFloatingArray subArr(long position, long count);

    public UpdatablePFloatingArray asUnresizable();
}
