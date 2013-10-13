package net.algart.arrays;

/**
 * <p>Resizable AlgART array of any numeric primitive elements (byte, short, int, long, float or double).</p>
 *
 * <p>Any class implementing this interface <b>must</b> implement one of
 * {@link MutableByteArray}, {@link MutableShortArray},
 * {@link MutableIntArray}, {@link MutableLongArray},
 * {@link MutableFloatArray}, {@link MutableDoubleArray},
 * subinterfaces.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutablePNumberArray extends UpdatablePNumberArray, MutablePArray {
}
