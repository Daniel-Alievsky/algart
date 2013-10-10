package net.algart.arrays;

/**
 * <p>Special version of {@link UpdatableObjectArray} allowing
 * to load an element without creating new Java object.</p>
 *
 * <p>The arrays of object created via some {@link MemoryModel memory models} may not implement this interface.
 * You may check, is this interface implemented, by <tt>intstanceof</tt> operator.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface UpdatableObjectInPlaceArray<E> extends ObjectInPlaceArray<E>, UpdatableObjectArray<E> {
    public UpdatableObjectInPlaceArray<E> subArray(long fromIndex, long toIndex);

    public UpdatableObjectInPlaceArray<E> subArr(long position, long count);

    public UpdatableObjectInPlaceArray<E> asUnresizable();
}
