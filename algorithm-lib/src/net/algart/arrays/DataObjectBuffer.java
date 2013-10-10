package net.algart.arrays;

/**
 * <p>Data buffer for <tt>Object</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataObjectBuffer<E> extends DataBuffer {
    public DataObjectBuffer<E> map(long position);

    public DataObjectBuffer<E> mapNext();

    public DataObjectBuffer<E> force();

    public E[] data();
}
