package net.algart.arrays;

/*Repeat.SectionStart all*/
/**
 * <p>Resizable AlgART array of <tt>float</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableFloatArray extends FloatStack, UpdatableFloatArray, MutablePFloatingArray {
    /*Repeat.SectionStart resultTypes*/
    public MutableFloatArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableFloatArray setData(long arrayPos, Object srcArray);

    public MutableFloatArray copy(Array src);

    public MutableFloatArray swap(UpdatableArray another);

    public MutableFloatArray length(long newLength);

    public MutableFloatArray ensureCapacity(long minCapacity);

    public MutableFloatArray trim();

    public MutableFloatArray append(Array appendedArray);

    public MutableFloatArray asCopyOnNextWrite();

    public MutableFloatArray shallowClone();
    /*Repeat.SectionEnd resultTypes*/
/*Repeat.SectionEnd all*/
}
