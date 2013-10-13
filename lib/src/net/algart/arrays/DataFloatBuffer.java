package net.algart.arrays;

/*Repeat.SectionStart all*/
/**
 * <p>Data buffer for <tt>float</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataFloatBuffer extends DataBuffer {
    public DataFloatBuffer map(long position);

    public DataFloatBuffer map(long position, boolean readData);

    public DataFloatBuffer mapNext();

    public DataFloatBuffer mapNext(boolean readData);

    public DataFloatBuffer map(long position, long maxCount);

    public DataFloatBuffer map(long position, long maxCount, boolean readData);

    public DataFloatBuffer force();

    public float[] data();
}
/*Repeat.SectionEnd all*/
