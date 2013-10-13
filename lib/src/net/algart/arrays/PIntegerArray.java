package net.algart.arrays;

/**
 * <p>AlgART array of any fixed-point primitive numeric elements (byte, short, int or long),
 * read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link ByteArray}, {@link ShortArray},
 * {@link IntArray}, {@link LongArray} subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface PIntegerArray extends PFixedArray, PNumberArray {
    public Class<? extends PIntegerArray> type();

    public Class<? extends UpdatablePIntegerArray> updatableType();

    public Class<? extends MutablePIntegerArray> mutableType();

    public PIntegerArray asImmutable();

    public PIntegerArray asTrustedImmutable();

    public MutablePIntegerArray mutableClone(MemoryModel memoryModel);

    public UpdatablePIntegerArray updatableClone(MemoryModel memoryModel);
}
