package net.algart.arrays;

/**
 * <p>AlgART array of any floating-point primitive elements (float or double), read-only access.</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link FloatArray}, {@link DoubleArray} subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface PFloatingArray extends PNumberArray {
    public Class<? extends PFloatingArray> type();

    public Class<? extends UpdatablePFloatingArray> updatableType();

    public Class<? extends MutablePFloatingArray> mutableType();

    public PFloatingArray asImmutable();

    public PFloatingArray asTrustedImmutable();

    public MutablePFloatingArray mutableClone(MemoryModel memoryModel);

    public UpdatablePFloatingArray updatableClone(MemoryModel memoryModel);
}
