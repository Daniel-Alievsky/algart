package net.algart.arrays;

/**
 * <p>AlgART array of any fixed-point primitive numeric, character or bit elements
 * (byte, short, int, long, char or boolean),
 * read/write access, no resizing.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link UpdatableBitArray}, {@link UpdatableCharArray},
 * {@link UpdatableByteArray}, {@link UpdatableShortArray},
 * {@link UpdatableIntArray}, {@link UpdatableLongArray}
 * subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatablePFixedArray extends PFixedArray, UpdatablePArray {
    public UpdatablePFixedArray subArray(long fromIndex, long toIndex);

    public UpdatablePFixedArray subArr(long position, long count);

    public UpdatablePFixedArray asUnresizable();
}
