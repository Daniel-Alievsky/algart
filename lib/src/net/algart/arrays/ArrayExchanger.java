package net.algart.arrays;

/**
 * <p>Exchanging interface, designed for exchanging (swapping) two elements in some data array.</p>
 *
 * <p>The basic method {@link #swap(long, long)}
 * of this interface works with indexes of the exchanged elements in the array.
 * So, every object, implementing this interface, is supposed to be working with some fixed linear data array.
 * The method of storing data in the array can be any; for example, it can be an
 * {@link UpdatableArray updatable AlgART array} or a usual Java array.
 * The length of the array is limited only by 2<sup>63</sup>&minus;1 (maximal possible value for <tt>long</tt>
 * indexes).</p>
 *
 * <p>This interface is used by {@link ArraySorter} class.</p>
 *
 * <p>Note: {@link UpdatableArray} interface extends this interface.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayExchanger {
    /**
     * Should exchange the elements at position <tt>firstIndex</tt> and <tt>secondIndex</tt> in the data array.
     *
     * @param firstIndex  index of the first exchanged element.
     * @param secondIndex index of the second exchanged element.
     */
    void swap(long firstIndex, long secondIndex);
}
